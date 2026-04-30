package zawr.currencymonitor.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import zawr.currencymonitor.service.TelegramNotificationService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class TelegramBotInitializer {

    private final TelegramNotificationService botService;
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private ScheduledExecutorService retryScheduler;
    private TelegramBotsApi botsApi; // Храним ссылку для корректного shutdown

    public TelegramBotInitializer(TelegramNotificationService botService) {
        this.botService = botService;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void startBotWithRetry() {
        retryScheduler = Executors.newSingleThreadScheduledExecutor();

        retryScheduler.scheduleAtFixedRate(() -> {
            // Если уже зарегистрирован — выходим, не создаём новые сессии
            if (registered.get()) {
                return;
            }

            try {
                // Создаём API только один раз
                if (botsApi == null) {
                    botsApi = new TelegramBotsApi(DefaultBotSession.class);
                }

                botsApi.registerBot(botService);
                registered.set(true); // ✅ Фиксируем успех
                log.info("Bot session started successfully");

            } catch (TelegramApiException e) {
                log.warn("Failed to start bot session, retrying in 30s...", e);
                // Сбрасываем botsApi, чтобы при следующей попытке создать новую
                botsApi = null;
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        // Корректно останавливаем планировщик
        if (retryScheduler != null && !retryScheduler.isShutdown()) {
            retryScheduler.shutdown();
            try {
                if (!retryScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    retryScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                retryScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("Telegram bot initializer shutdown complete");
    }
}