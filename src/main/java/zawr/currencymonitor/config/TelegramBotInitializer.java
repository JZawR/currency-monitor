package zawr.currencymonitor.config;

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
    private final AtomicBoolean running = new AtomicBoolean(true);

    private final TelegramNotificationService botService;

    public TelegramBotInitializer(TelegramNotificationService botService) {
        this.botService = botService;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void startBotWithRetry() {
        ScheduledExecutorService retryScheduler = Executors.newSingleThreadScheduledExecutor();
        retryScheduler.scheduleAtFixedRate(() -> {
            try {
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                botsApi.registerBot(botService);
                running.set(true);
                log.info("Bot session started successfully");
            } catch (TelegramApiException e) {
                log.warn("Failed to start bot session, retrying in 30s...", e);
                // session will be retried on next tick
            }
        }, 0, 30, TimeUnit.SECONDS);
    }
}