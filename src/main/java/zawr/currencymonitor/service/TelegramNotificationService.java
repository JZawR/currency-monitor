package zawr.currencymonitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import zawr.currencymonitor.entity.CurrencyRateEntity;
import zawr.currencymonitor.properties.AppProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class TelegramNotificationService extends TelegramLongPollingBot {

    private final AppProperties appProperties;
    private final ThresholdService thresholdService;
    private final CurrencyRateService currencyRateService;

    private static final Pattern SET_COMMAND_PATTERN = Pattern.compile("^/set\\s+([0-9]+(?:\\.[0-9]+)?)$");

    public TelegramNotificationService(AppProperties appProperties, ThresholdService thresholdService, CurrencyRateService currencyRateService) {
        super(appProperties.getTelegram().getBotToken());
        this.appProperties = appProperties;
        this.thresholdService = thresholdService;
        this.currencyRateService = currencyRateService;
        log.info("Telegram bot initialized with threshold management");
    }

    @Override
    public String getBotUsername() {
        return appProperties.getTelegram().getBotUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {

            if (!update.hasMessage() || !update.getMessage().hasText()) {
                return;
            }

            Message message = update.getMessage();
            String chatId = message.getChatId().toString();
            String text = message.getText().trim();

            log.debug("Received message from chat {}: {}", chatId, text);

            if (text.startsWith("/start") || text.equalsIgnoreCase("привет")) {
                sendWelcomeMessage(chatId);
            } else if (text.startsWith("/help")) {
                sendHelpMessage(chatId);
            } else if (text.startsWith("/set")) {
                handleSetCommand(chatId, text);
            } else if (text.equalsIgnoreCase("/get") || text.equalsIgnoreCase("/threshold")) {
                handleGetCommand(chatId);
            } else if (text.equalsIgnoreCase("/off") || text.equalsIgnoreCase("/disable")) {
                handleDisableCommand(chatId);
            } else {
                // Игнорируем остальные сообщения или можно добавить fallback
                log.debug("Unknown command from chat {}: {}", chatId, text);
            }
        } catch (Exception e) {
            log.error("Ошибка обработки апдейта", e);
        }
    }

    private void sendWelcomeMessage(String chatId) {
        String text = """
                👋 Привет! Я бот для отслеживания курса доллара.
                
                📋 Доступные команды:
                /set <значение> — установить порог уведомления (например, /set 95.50)
                /get — посмотреть текущий порог
                /off — отключить уведомления
                /help — показать эту справку
                
                Как только курс доллара достигнет вашего порога, я отправлю уведомление! 🚀
                """;
        sendMarkdownMessage(chatId, text);
    }

    private void sendHelpMessage(String chatId) {
        sendWelcomeMessage(chatId); // Переиспользуем приветственное сообщение
    }

    private void handleSetCommand(String chatId, String command) {
        Matcher matcher = SET_COMMAND_PATTERN.matcher(command);

        if (!matcher.matches()) {
            sendMarkdownMessage(chatId, "❌ Неверный формат команды.\nИспользуйте: `/set 95.50`");
            return;
        }

        try {
            BigDecimal threshold = new BigDecimal(matcher.group(1));
            thresholdService.setThreshold(chatId, threshold);

            String response = String.format(
                    "✅ Порог установлен: *%.2f RUB*\n" +
                            "🔔 Вы получите уведомление, когда курс доллара достигнет этого значения.",
                    threshold.doubleValue());
            sendMarkdownMessage(chatId, response);
            log.info("Threshold set for chat {}: {}", chatId, threshold);

        } catch (NumberFormatException e) {
            sendMarkdownMessage(chatId, "❌ Ошибка: введите корректное числовое значение.");
        } catch (IllegalArgumentException e) {
            sendMarkdownMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }

    private void handleGetCommand(String chatId) {
        Optional<BigDecimal> thresholdOpt = thresholdService.getThreshold(chatId);
        StringBuilder allBanks = new StringBuilder();
        List<CurrencyRateEntity> latestByEachBank = currencyRateService.findLatestByEachBank();
        System.out.println("latestByEachBank: " + latestByEachBank);
        latestByEachBank.forEach(bank -> allBanks.append(bank.getBank()).append(": ").append(bank.getSell()).append("\n"));


        if (thresholdOpt.isPresent()) {
            String text = String.format(
                    "текущий курс: \n" + allBanks +
                    "📊 Ваш текущий порог: *%.2f RUB*\n" +
                            "🔔 Уведомления: *включены*",
                    thresholdOpt.get().doubleValue());
            sendMarkdownMessage(chatId, text);
        } else {
            sendMarkdownMessage(chatId, "⚠️ У вас не установлен порог.\nИспользуйте команду `/set 95.50` для настройки.");
        }
    }

    private void handleDisableCommand(String chatId) {
        thresholdService.disableNotifications(chatId);
        sendMarkdownMessage(chatId, "🔕 Уведомления *отключены*.\nДля включения снова используйте `/set <значение>`.");
    }

    private void sendMarkdownMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");

        try {
            execute(message);
            log.info("Telegram message sent to chat {}: {}", chatId, text.replaceAll("\\n", " "));
        } catch (TelegramApiException e) {
            log.error("Failed to send Telegram message to chat {}", chatId, e);
        }
    }

    /**
     * Отправляет уведомление всем пользователям, у которых порог достигнут
     *
     * @param currentRate текущий курс USD
     * @param message     текст уведомления
     */
    public void broadcastRateAlert(BigDecimal currentRate, String message) {
        // В реальной реализации здесь должен быть запрос ко всем активным пользователям
        // Например: thresholdRepository.findAllByEnabledTrue()
        // Для примера пока заглушка:
        log.info("Checking alerts for rate: {}", currentRate);
        // TODO: реализовать массовую проверку и рассылку
    }

    public void sendUsdRateAlert(String message, String chatId) {
        sendMarkdownMessage(chatId, message);
    }

    public void sendInfoMessage(String text, String chatId) {
        sendMarkdownMessage(chatId, text);
    }
}