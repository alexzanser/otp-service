package com.otpservice.service.delivery;
import com.otpservice.model.OtpCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
/**
 * Фабрика для создания сервисов доставки OTP кодов
 */
public class OtpDeliveryServiceFactory {
    private static final Logger logger = LoggerFactory.getLogger(OtpDeliveryServiceFactory.class);
    private static OtpDeliveryServiceFactory instance;
    private final Map<OtpCode.DeliveryChannel, OtpDeliveryService> deliveryServices = new HashMap<>();
    private OtpDeliveryServiceFactory() {
    }
    /**
     * Получить экземпляр фабрики
     */
    public static synchronized OtpDeliveryServiceFactory getInstance() {
        if (instance == null) {
            instance = new OtpDeliveryServiceFactory();
        }
        return instance;
    }
    /**
     * Получить сервис доставки по типу канала
     * 
     * @param channel Канал доставки
     * @return Сервис доставки OTP
     */
    public OtpDeliveryService getDeliveryService(OtpCode.DeliveryChannel channel) {
        if (deliveryServices.containsKey(channel)) {
            return deliveryServices.get(channel);
        }
        OtpDeliveryService service;
        switch (channel) {
            case SMS:
                service = new SmsOtpDeliveryService();
                break;
            case EMAIL:
                service = new EmailOtpDeliveryService();
                break;
            case TELEGRAM:
                service = new TelegramOtpDeliveryService();
                service.initialize();
                break;
            case FILE:
                service = new FileOtpDeliveryService();
                break;
            default:
                logger.error("Unknown delivery channel: {}", channel);
                throw new IllegalArgumentException("Unknown delivery channel: " + channel);
        }
        deliveryServices.put(channel, service);
        logger.info("Created delivery service for channel: {}", channel);
        return service;
    }
    /**
     * Закрыть все сервисы доставки при завершении работы приложения
     */
    public void shutdownAll() {
        logger.info("Shutting down all delivery services");
        for (Map.Entry<OtpCode.DeliveryChannel, OtpDeliveryService> entry : deliveryServices.entrySet()) {
            try {
                logger.debug("Shutting down {} delivery service", entry.getKey());
                entry.getValue().shutdown();
            } catch (Exception e) {
                logger.error("Error shutting down {} delivery service", entry.getKey(), e);
            }
        }
        deliveryServices.clear();
    }
} 