package zawr.currencymonitor.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import zawr.currencymonitor.dto.GraphQLResponse;
import zawr.currencymonitor.entity.CurrencyRateEntity;
import zawr.currencymonitor.entity.UserThreshold;
import zawr.currencymonitor.model.AlertState;
import zawr.currencymonitor.model.CurrencyRateMapper;
import zawr.currencymonitor.model.SovcombankCurrencyRate;
import zawr.currencymonitor.properties.AppProperties;
import zawr.currencymonitor.repository.AlertStateRepository;
import zawr.currencymonitor.repository.UserThresholdRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class CurrencyRateScheduler {

    private final SovcombankApiService apiService;
    private final AlertStateRepository alertRepository;
    private final TelegramNotificationService telegramService;
    private final AppProperties appProperties;
    private final UserThresholdRepository thresholdRepository;
    private final BbrApiService bbrApiService;
    private final CurrencyRateService currencyRateService;

    @Scheduled(cron = "${app.scheduler.cron}", scheduler = "schedulerTaskExecutor")
    @Transactional
    public void checkUsdRate() {
        log.info("Starting scheduled currency rate check...");

        List<CurrencyRateEntity> rates = new ArrayList<>();
        rates.addAll(getBbrRates());
        rates.addAll(getSovcombankRates());
        if (rates.isEmpty()) {
            log.warn("No rates received from API");
            return;
        }

        try {
            rates.stream()
                    .filter(rate -> "USD".equalsIgnoreCase(rate.getBase()))
                    .forEach(currencyRateService::save);
        } catch (Exception e) {
            log.warn("Error while saving currency rates", e);
        }


        rates.stream()
                .min(Comparator.comparing(CurrencyRateEntity::getSell))
                .ifPresent(this::checkAndNotifyAllUsers);
    }

    private List<CurrencyRateEntity> getBbrRates() {
        log.info("trying get bbr bank currency rates");
        List<GraphQLResponse.RateElement> bbrRates = Collections.emptyList();
        try {
            bbrRates = bbrApiService.fetchRates("krasnodar", 10, "CASH_EXCHANGE").block(Duration.ofSeconds(5));
        } catch (Exception e) {
            log.error("Error while fetching bbrRates ", e);
        }
        if (CollectionUtils.isEmpty(bbrRates)) {
            log.warn("No bbrRates received from API");
            return Collections.emptyList();
        }
        return bbrRates.stream()
                .filter(r -> "USD".equalsIgnoreCase(r.fromCurrency().code()))
                .map(CurrencyRateMapper::BbrModelToEntity)
                .peek(r -> log.info("bbr bank rate: {}", r.getSell()))
                .toList();
    }

    private List<CurrencyRateEntity> getSovcombankRates() {
        log.info("trying get sovcombank currency rates");
        List<SovcombankCurrencyRate> sovcombankRates = Collections.emptyList();
        try {
            sovcombankRates = apiService.fetchCurrencyRates();
        } catch (Exception e) {
            log.error("Error while fetching sovcombankRates ", e);
        }
        if (CollectionUtils.isEmpty(sovcombankRates)) {
            log.warn("No sovcombankRates received from API");
            return Collections.emptyList();
        }
        return sovcombankRates.stream()
                .filter(r -> "USD".equalsIgnoreCase(r.getBase()))
                .map(CurrencyRateMapper::SovcombankModelToEntity)
                .peek(r -> log.info("sovcombank rate: {}", r.getSell()))
                .toList();
    }

    /**
     * Проверяет курс для всех пользователей с активными порогами
     */
    @Transactional
    public void checkAndNotifyAllUsers(CurrencyRateEntity rate) {
        Double currentSell = Double.valueOf(rate.getSell());
        String bank = rate.getBank();

        // Получаем всех пользователей с активными порогами
        List<UserThreshold> activeThresholds = thresholdRepository.findAllByEnabledTrue();
        if (activeThresholds.isEmpty()) {
            log.debug("No active user thresholds found");
            return;
        }

        log.debug("Checking {} active user thresholds for rate {}", activeThresholds.size(), currentSell);

        for (UserThreshold userThreshold : activeThresholds) {
            checkAndNotifyUser(bank, userThreshold, currentSell);
        }
    }

    /**
     * Проверяет порог для конкретного пользователя и отправляет уведомление при необходимости
     */

    protected void checkAndNotifyUser(String bank, UserThreshold userThreshold, Double currentSell) {
        String chatId = userThreshold.getTelegramChatId();
        BigDecimal threshold = userThreshold.getUsdThreshold();
        Double thresholdDouble = threshold.doubleValue();

        Boolean antiSpam = appProperties.getTelegram().getAntiSpamEnabled();
        Long cooldownMinutes = appProperties.getTelegram().getAlertCooldownMinutes();

        // Ключ алерта теперь включает chatId для персонализации
        String alertKey = makeAlertKey(chatId, thresholdDouble);

        Optional<AlertState> stateOpt = alertRepository.findByKey(alertKey);

        AlertState state = stateOpt.orElseGet(() -> {
            var newState = AlertState.builder()
                    .key(alertKey)
                    .telegramChatId(chatId)
                    .base("USD")
                    .quot("RUB")
                    .rateType("SELL")
                    .threshold(thresholdDouble)
                    .alertSent(false)
                    .lastCheckedAt(LocalDateTime.now())
                    .lastCheckedRate(currentSell)
                    .build();
            return alertRepository.save(newState);
        });

        // Обновляем время последней проверки
        state.setLastCheckedAt(LocalDateTime.now());
        state.setLastCheckedRate(currentSell);

        boolean shouldNotify = false;
        String reason = "";

        if (currentSell < thresholdDouble) {
            // Курс ниже порога пользователя
            if (!antiSpam) {
                shouldNotify = true;
                reason = "anti-spam disabled";
            } else if (!state.isAlertSent()) {
                shouldNotify = true;
                reason = "first drop below threshold";
            } else if (state.getLastAlertAt() != null) {
                long minutesSinceLastAlert = Duration
                        .between(state.getLastAlertAt(), LocalDateTime.now())
                        .toMinutes();

                if (minutesSinceLastAlert >= cooldownMinutes) {
                    shouldNotify = true;
                    reason = "cooldown expired (" + minutesSinceLastAlert + " min)";
                } else {
                    reason = "cooldown active (" + minutesSinceLastAlert + "/" + cooldownMinutes + " min)";
                }
            }

            if (shouldNotify) {
                sendAlertToUser(currentSell, bank, thresholdDouble, reason, chatId);
                state.setAlertSent(true);
                state.setLastAlertAt(LocalDateTime.now());
            }
        } else {
            // Курс выше или равен порогу — сбрасываем флаг
            if (state.isAlertSent()) {
                log.info("Rate recovered for chat {} ({} >= {}), resetting alert flag",
                        chatId, currentSell, thresholdDouble);
                state.setAlertSent(false);
                telegramService.sendInfoMessage(
                        String.format("✅ *Курс восстановился*\nUSD/RUB (sell): *%.2f* >= %.2f",
                                currentSell, thresholdDouble),
                        chatId
                );
            }
        }

        alertRepository.save(state);
        log.debug("Alert check for chat {}: rate={}, threshold={}, notify={}, reason={}",
                chatId, currentSell, thresholdDouble, shouldNotify, reason);
    }

    /**
     * Формирует уникальный ключ алерта с учётом chatId
     */
    private String makeAlertKey(String chatId, Double threshold) {
        return String.format("%s:%s/%s:%s:%.2f", chatId, "USD", "RUB", "SELL", threshold);
    }

    private void sendAlertToUser(Double currentSell, String bank, Double threshold, String reason, String chatId) {
        String message = String.format(
                """
                        🔔 *Внимание! Курс упал!*
                        💵 USD/RUB (sell): *%.2f*
                        Банк: %s
                        📉 Ваш порог: %.2f
                        ⏰ %s
                        🔍 Причина: %s""",
                currentSell, bank, threshold,
                LocalDateTime.now(),
                reason
        );
        telegramService.sendUsdRateAlert(message, chatId);
        log.info("Alert sent to chat {}: USD sell {} < {} (reason: {})",
                chatId, currentSell, threshold, reason);
    }

    @Transactional
    @PostConstruct
    public void checkOnStartup() {
        log.info("Running initial currency check on startup...");
        checkUsdRate();
    }
}