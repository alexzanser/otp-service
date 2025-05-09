package com.otpservice.service.delivery;
/**
 * Интерфейс для сервисов отправки OTP-кодов
 */
public interface OtpDeliveryService {
    /**
     * Отправляет OTP-код
     * 
     * @param recipient Получатель (телефон, email, Telegram ID и т.д.)
     * @param code OTP-код для отправки
     * @return true если отправка успешна, false в противном случае
     */
    boolean sendOtp(String recipient, String code);
    /**
     * Подготавливает канал доставки (если необходимо)
     */
    default void initialize() {
    }
    /**
     * Освобождает ресурсы канала доставки (если необходимо)
     */
    default void shutdown() {
    }
    /**
     * Проверяет возможность доставки OTP-кода получателю
     * 
     * @param recipient Получатель
     * @return true если доставка возможна, false в противном случае
     */
    default boolean canDeliver(String recipient) {
        return true;
    }
} 