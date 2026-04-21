package zawr.currencymonitor.service;

import jakarta.annotation.PostConstruct;
import zawr.currencymonitor.model.CurrencyRate;
import zawr.currencymonitor.model.AlertState;
import zawr.currencymonitor.properties.AppProperties;
import zawr.currencymonitor.repository.CurrencyRateRepository;
import zawr.currencymonitor.repository.AlertStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class CurrencyRateScheduler {

    private final SovcombankApiService apiService;
    private final CurrencyRateRepository currencyRepository;
    private final AlertStateRepository alertRepository;  // ← новый
    private final TelegramNotificationService telegramService;
    private final AppProperties appProperties;

    @Scheduled(cron = "${app.scheduler.cron}")
    public void checkUsdRate() {
        log.info("Starting scheduled currency rate check...");

        List<CurrencyRate> rates = apiService.fetchCurrencyRates();
        if (rates.isEmpty()) {
            log.warn("No rates received from API");
            return;
        }

        rates.stream()
                .filter(rate -> "USD".equalsIgnoreCase(rate.getBase()))
                .forEach(rate -> {
                    rate.generateUniqueId();
                    currencyRepository.save(rate);
                });

// Проверяем USD/RUB sell
        rates.stream()
                .filter(r -> "USD".equalsIgnoreCase(r.getBase()) && "RUB".equalsIgnoreCase(r.getQuot()))
                .findFirst()
                .ifPresent(this::checkAndNotify);

        log.info("Scheduled check completed: {}", rates);
    }

    /**
     * Основная логика проверки с защитой от спама
     */
    private void checkAndNotify(CurrencyRate rate) {
        Double currentSell = rate.getSellAsDouble();
        if (currentSell == null) {
            log.warn("Could not parse sell rate: {}", rate.getSell());
            return;
        }

        var tgProps = appProperties.getTelegram();
        Double threshold = tgProps.getThresholdUsdSell();
        Boolean antiSpam = tgProps.getAntiSpamEnabled();
        Long cooldownMinutes = tgProps.getAlertCooldownMinutes();

        String alertKey = AlertState.makeKey(rate.getBase(), rate.getQuot(), "SELL", threshold);
        Optional<AlertState> stateOpt = alertRepository.findByKey(alertKey);

        AlertState state = stateOpt.orElseGet(() -> {
            var newState = AlertState.builder()
                    .key(alertKey)
                    .base(rate.getBase())
                    .quot(rate.getQuot())
                    .rateType("SELL")
                    .threshold(threshold)
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

        if (currentSell < threshold) {
            // Курс ниже порога
            if (!antiSpam) {
                // Анти-спам выключен — уведомляем всегда
                shouldNotify = true;
                reason = "anti-spam disabled";
            } else if (!state.isAlertSent()) {
                // Первое падение — уведомляем
                shouldNotify = true;
                reason = "first drop below threshold";
            } else if (state.getLastAlertAt() != null) {
                // Уже уведомляли — проверяем кулдаун
                long minutesSinceLastAlert = java.time.Duration
                        .between(state.getLastAlertAt(), LocalDateTime.now())
                        .toMinutes();

                if (minutesSinceLastAlert >= cooldownMinutes) {
                    // Кулдаун прошёл — можно снова уведомить
                    shouldNotify = true;
                    reason = "cooldown expired (" + minutesSinceLastAlert + " min)";
                } else {
                    reason = "cooldown active (" + minutesSinceLastAlert + "/" + cooldownMinutes + " min)";
                }
            }

            if (shouldNotify) {
                sendAlert(rate, currentSell, threshold, reason);
                state.setAlertSent(true);
                state.setLastAlertAt(LocalDateTime.now());
            }
        } else {
            // Курс выше или равен порогу — сбрасываем флаг
            if (state.isAlertSent()) {
                log.info("Rate recovered ({} >= {}), resetting alert flag", currentSell, threshold);
                state.setAlertSent(false);
                // Опционально: отправить уведомление о восстановлении
                telegramService.sendInfoMessage(
                        String.format("✅ *Курс восстановился*\nUSD/RUB (sell): *%.2f* >= %.2f",
                                currentSell, threshold)
                );
            }
        }

        alertRepository.save(state);
        log.debug("Alert check: rate={}, threshold={}, notify={}, reason={}",
                currentSell, threshold, shouldNotify, reason);
    }

    private void sendAlert(CurrencyRate rate, Double currentSell, Double threshold, String reason) {
        String message = String.format(
                "🔔 *Внимание! Курс упал!*\n" +
                        "💵 USD/RUB (sell): *%.2f*\n" +
                        "📉 Порог: %.2f\n" +
                        "⏰ %s\n" +
                        "🔍 Причина: %s",
                currentSell, threshold,
                java.time.LocalDateTime.now(),
                reason
        );
        telegramService.sendUsdRateAlert(currentSell, threshold);
        log.info("Alert sent: USD sell {} < {} (reason: {})", currentSell, threshold, reason);
    }

    // Очистка старой истории (оставляем как было)
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldHistory() {
        var retentionDays = appProperties.getHistory().getRetentionDays();
        var cutoff = LocalDateTime.now().minusDays(retentionDays);
        // ... реализация очистки ...
    }

    @PostConstruct
    public void checkOnStartup() {
        log.info("Running initial currency check on startup...");
        checkUsdRate();
    }
}