package com.otpservice.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static final Properties properties = new Properties();
    static {
        try (InputStream inputStream = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (inputStream == null) {
                throw new RuntimeException("Property file not found in classpath");
            }
            properties.load(inputStream);
            logger.info("Application properties loaded successfully");
        } catch (IOException e) {
            logger.error("Error loading application properties", e);
            throw new RuntimeException("Error loading application properties", e);
        }
    }
    public static String getJwtSecret() {
        return properties.getProperty("jwt.secret");
    }
    public static long getJwtExpiration() {
        return Long.parseLong(properties.getProperty("jwt.expiration", "86400000"));
    }
    public static int getDefaultOtpLength() {
        return Integer.parseInt(properties.getProperty("otp.default.length", "6"));
    }
    public static int getDefaultOtpExpirationMs() {
        return Integer.parseInt(properties.getProperty("otp.default.expiration", "300000"));
    }
    public static String getMailSmtpHost() {
        return properties.getProperty("mail.smtp.host");
    }
    public static int getMailSmtpPort() {
        return Integer.parseInt(properties.getProperty("mail.smtp.port", "587"));
    }
    public static boolean getMailSmtpAuth() {
        return Boolean.parseBoolean(properties.getProperty("mail.smtp.auth", "true"));
    }
    public static boolean getMailSmtpStartTlsEnable() {
        return Boolean.parseBoolean(properties.getProperty("mail.smtp.starttls.enable", "true"));
    }
    public static String getMailUsername() {
        return properties.getProperty("mail.username");
    }
    public static String getMailPassword() {
        return properties.getProperty("mail.password");
    }
    public static String getTelegramBotUsername() {
        return properties.getProperty("telegram.bot.username");
    }
    public static String getTelegramBotToken() {
        return properties.getProperty("telegram.bot.token");
    }
    public static String getServerHost() {
        return properties.getProperty("server.host", "localhost");
    }
    public static int getServerPort() {
        return Integer.parseInt(properties.getProperty("server.port", "8080"));
    }
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
} 