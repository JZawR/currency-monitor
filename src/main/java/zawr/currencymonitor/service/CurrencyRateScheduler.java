package zawr.currencymonitor.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import zawr.currencymonitor.entity.UserThreshold;
import zawr.currencymonitor.model.AlertState;
import zawr.currencymonitor.model.CurrencyRate;
import zawr.currencymonitor.model.CurrencyRateMapper;
import zawr.currencymonitor.properties.AppProperties;
import zawr.currencymonitor.repository.AlertStateRepository;
import zawr.currencymonitor.repository.CurrencyRateRepository;
import zawr.currencymonitor.repository.UserThresholdRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class CurrencyRateScheduler {

    private final SovcombankApiService apiService;
    private final CurrencyRateRepository currencyRepository;
    private final AlertStateRepository alertRepository;
    private final TelegramNotificationService telegramService;
    private final AppProperties appProperties;
    private final UserThresholdRepository thresholdRepository;

    @Scheduled(cron = "${app.scheduler.cron}")
    @Transactional
    public void checkUsdRate() {
        log.info("Starting scheduled currency rate check...");

        List<CurrencyRate> rates = apiService.fetchCurrencyRates();
        if (rates.isEmpty()) {
            log.warn("No rates received from API");
            return;
        }

        // Сохраняем все курсы USD
        rates.stream()
                .filter(rate -> "USD".equalsIgnoreCase(rate.getBase()))
                .forEach(rate -> {
                    rate.generateUniqueId();
                    currencyRepository.save(CurrencyRateMapper.modelToEntity(rate));
                });

        // Находим курс USD/RUB sell и проверяем пороги пользователей
        rates.stream()
                .filter(r -> "USD".equalsIgnoreCase(r.getBase()) && "RUB".equalsIgnoreCase(r.getQuot()))
                .findFirst()
                .ifPresent(this::checkAndNotifyAllUsers);
    }

    /**
     * Проверяет курс для всех пользователей с активными порогами
     */
    @Transactional
    public void checkAndNotifyAllUsers(CurrencyRate rate) {
        Double currentSell = rate.getSellAsDouble();
        if (currentSell == null) {
            log.warn("Could not parse sell rate: {}", rate.getSell());
            return;
        }

        // Получаем всех пользователей с активными порогами
        List<UserThreshold> activeThresholds = thresholdRepository.findAllByEnabledTrue();
        if (activeThresholds.isEmpty()) {
            log.debug("No active user thresholds found");
            return;
        }

        log.debug("Checking {} active user thresholds for rate {}", activeThresholds.size(), currentSell);

        for (UserThreshold userThreshold : activeThresholds) {
            checkAndNotifyUser(userThreshold, currentSell);
        }
    }

    /**
     * Проверяет порог для конкретного пользователя и отправляет уведомление при необходимости
     */

    protected void checkAndNotifyUser(UserThreshold userThreshold, Double currentSell) {
        String chatId = userThreshold.getTelegramChatId();
        BigDecimal threshold = userThreshold.getUsdThreshold();
        Double thresholdDouble = threshold.doubleValue();

        Boolean antiSpam = appProperties.getTelegram().getAntiSpamEnabled();
        Long cooldownMinutes = appProperties.getTelegram().getAlertCooldownMinutes();

        // Ключ алерта теперь включает chatId для персонализации
        String alertKey = makeAlertKey(chatId, "USD", "RUB", "SELL", thresholdDouble);

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
                long minutesSinceLastAlert = java.time.Duration
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
                sendAlertToUser(currentSell, thresholdDouble, reason, chatId);
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
    private String makeAlertKey(String chatId, String base, String quot, String rateType, Double threshold) {
        return String.format("%s:%s/%s:%s:%.2f", chatId, base, quot, rateType, threshold);
    }

    private void sendAlertToUser(Double currentSell, Double threshold, String reason, String chatId) {
        String message = String.format(
                """
                        🔔 *Внимание! Курс упал!*
                        💵 USD/RUB (sell): *%.2f*
                        📉 Ваш порог: %.2f
                        ⏰ %s
                        🔍 Причина: %s""",
                currentSell, threshold,
                java.time.LocalDateTime.now(),
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