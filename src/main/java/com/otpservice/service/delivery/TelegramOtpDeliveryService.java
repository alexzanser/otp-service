package com.otpservice.service.delivery;
import com.otpservice.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import java.util.concurrent.ConcurrentHashMap;
/**
 * Сервис для отправки OTP-кодов через Telegram бота
 */
public class TelegramOtpDeliveryService implements OtpDeliveryService {
    private static final Logger logger = LoggerFactory.getLogger(TelegramOtpDeliveryService.class);
    private static final String OTP_MESSAGE_TEMPLATE = "Ваш OTP-код: %s";
    private OtpBot bot;
    private boolean initialized = false;
    @Override
    public void initialize() {
        try {
            logger.info("Initializing Telegram OTP delivery service");
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            bot = new OtpBot(AppConfig.getTelegramBotToken(), AppConfig.getTelegramBotUsername());
            botsApi.registerBot(bot);
            initialized = true;
            logger.info("Telegram OTP delivery service initialized successfully");
        } catch (TelegramApiException e) {
            logger.error("Failed to initialize Telegram OTP delivery service", e);
            initialized = false;
        }
    }
    @Override
    public boolean sendOtp(String recipient, String code) {
        if (!initialized) {
            initialize();
            if (!initialized) {
                logger.error("Cannot send OTP via Telegram: service not initialized");
                return false;
            }
        }
        logger.info("Sending OTP code via Telegram to chat ID: {}", recipient);
        try {
            Long chatId = Long.parseLong(recipient);
            String message = String.format(OTP_MESSAGE_TEMPLATE, code);
            return bot.sendOtpMessage(chatId, message);
        } catch (NumberFormatException e) {
            logger.error("Invalid Telegram chat ID: {}", recipient);
            return false;
        }
    }
    @Override
    public void shutdown() {
        logger.info("Shutting down Telegram OTP delivery service");
        if (bot != null) {
            bot.onClosing();
        }
        initialized = false;
    }
    @Override
    public boolean canDeliver(String recipient) {
        try {
            Long.parseLong(recipient);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    /**
     * Внутренний класс для реализации Telegram бота
     */
    private static class OtpBot extends TelegramLongPollingBot {
        private final Logger botLogger = LoggerFactory.getLogger(OtpBot.class);
        private final String botToken;
        private final String botUsername;
        private final ConcurrentHashMap<Long, String> users = new ConcurrentHashMap<>();
        public OtpBot(String botToken, String botUsername) {
            super(botToken);
            this.botToken = botToken;
            this.botUsername = botUsername;
        }
        @Override
        public String getBotToken() {
            return botToken;
        }
        @Override
        public String getBotUsername() {
            return botUsername;
        }
        @Override
        public void onUpdateReceived(Update update) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                Message message = update.getMessage();
                Long chatId = message.getChatId();
                String userName = message.getChat().getUserName() != null ? 
                        message.getChat().getUserName() : message.getChat().getFirstName();
                users.put(chatId, userName);
                String text = message.getText();
                botLogger.info("Received message from {}: {}", chatId, text);
                if ("/start".equals(text)) {
                    sendMessage(chatId, "Добро пожаловать! Этот бот используется для отправки OTP-кодов." +
                            "\nВаш chat_id: " + chatId);
                } else {
                    sendMessage(chatId, "Я только отправляю OTP-коды и не могу обрабатывать другие сообщения." +
                            "\nВаш chat_id: " + chatId);
                }
            }
        }
        public boolean sendOtpMessage(Long chatId, String message) {
            try {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId.toString());
                sendMessage.setText(message);
                execute(sendMessage);
                botLogger.info("OTP message sent to chat ID: {}", chatId);
                return true;
            } catch (TelegramApiException e) {
                botLogger.error("Error sending OTP message to chat ID: {}", chatId, e);
                return false;
            }
        }
        private void sendMessage(Long chatId, String text) {
            try {
                SendMessage message = new SendMessage();
                message.setChatId(chatId.toString());
                message.setText(text);
                execute(message);
            } catch (TelegramApiException e) {
                botLogger.error("Error sending message to chat ID: {}", chatId, e);
            }
        }
    }
} 