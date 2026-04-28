package zawr.currencymonitor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import zawr.currencymonitor.service.TelegramNotificationService;

@Component
@Slf4j
public class TelegramBotInitializer {

    private final TelegramNotificationService botService;

    public TelegramBotInitializer(TelegramNotificationService botService) {
        this.botService = botService;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(botService);
            log.info("Telegram bot successfully registered and polling started");
        } catch (TelegramApiException e) {
            log.error("Failed to register Telegram bot: {}", e.getMessage(), e);
        }
    }
}