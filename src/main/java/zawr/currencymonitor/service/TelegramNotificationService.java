package zawr.currencymonitor.service;

import zawr.currencymonitor.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramNotificationService extends TelegramLongPollingBot {

    private final AppProperties appProperties;

    @Override
    public void onUpdateReceived(Update update) {
        // Не обрабатываем входящие сообщения, только отправляем уведомления
    }

    @Override
    public String getBotUsername() {
        return appProperties.getTelegram().getBotUsername(); // опционально
    }

    @Override
    public String getBotToken() {
        return appProperties.getTelegram().getBotToken();
    }

    public void sendUsdRateAlert(Double currentRate, Double threshold) {
        String message = String.format(
                "🔔 *Внимание! Курс упал!*\n" +
                        "💵 USD/RUB (sell): *%.2f*\n" +
                        "📉 Порог: %.2f\n" +
                        "⏰ %s",
                currentRate, threshold, java.time.LocalDateTime.now()
        );

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