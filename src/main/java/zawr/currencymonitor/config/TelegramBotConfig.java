package zawr.currencymonitor.config;

import zawr.currencymonitor.service.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.telegram", name = "bot-token")
public class TelegramBotConfig {

    private final TelegramNotificationService telegramService;

    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        // Ручная регистрация бота для совместимости со Spring Boot 3 [[31]]
        var botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(telegramService);
        return botsApi;
    }
}