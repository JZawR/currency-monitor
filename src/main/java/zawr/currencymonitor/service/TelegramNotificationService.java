package zawr.currencymonitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import zawr.currencymonitor.properties.AppProperties;

@Service
@Slf4j
public class TelegramNotificationService extends TelegramLongPollingBot {
    private final AppProperties appProperties;

    public TelegramNotificationService(AppProperties appProperties) {
        super(appProperties.getTelegram().getBotToken());
        this.appProperties = appProperties;

        // Небольшая задержка для гарантии инициализации
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Telegram bot initialized");
    }

    @Override
    public String getBotUsername() {
        String username = appProperties.getTelegram().getBotUsername();
        log.debug("getBotUsername() called, returning: '{}'", username);
        return username;
    }

    @Override
    public String getBotToken() {
        String token = appProperties.getTelegram().getBotToken();
        log.debug("getBotToken() called, returning: '{}'", token != null ? "***" + token.substring(Math.max(0, token.length() - 5)) : "null");
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Не обрабатываем входящие сообщения, только отправляем уведомления
    }

    public void sendUsdRateAlert(String message) {
        sendMarkdownMessage(appProperties.getTelegram().getChatId(), message);
    }

    public void sendInfoMessage(String text) {
        sendMarkdownMessage(appProperties.getTelegram().getChatId(), text);
    }

    private void sendMarkdownMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");

        try {
            execute(message);
            log.info("Telegram notification sent: {}", text);
        } catch (TelegramApiException e) {
            log.error("Failed to send Telegram message", e);
        }
    }
}