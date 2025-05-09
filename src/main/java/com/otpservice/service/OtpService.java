package com.otpservice.service;
import com.otpservice.dao.OtpCodeDao;
import com.otpservice.model.OtpCode;
import com.otpservice.model.OtpConfig;
import com.otpservice.model.User;
import com.otpservice.service.delivery.OtpDeliveryService;
import com.otpservice.service.delivery.OtpDeliveryServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
public class OtpService {
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String DIGITS = "0123456789";
    private final OtpCodeDao otpCodeDao;
    private final OtpConfigService otpConfigService;
    private final UserService userService;
    public OtpService() {
        this.otpCodeDao = new OtpCodeDao();
        this.otpConfigService = new OtpConfigService();
        this.userService = new UserService();
    }
    /**
     * Генерирует и отправляет OTP код
     * 
     * @param userId ID пользователя
     * @param operationId ID операции
     * @param recipient Получатель кода
     * @param deliveryChannel Канал доставки
     * @return Сгенерированный OTP код или null в случае ошибки
     */
    public OtpCode generateAndSendOtp(Long userId, String operationId, String recipient, OtpCode.DeliveryChannel deliveryChannel) {
        logger.info("Generating OTP for user ID: {}, operation: {}, channel: {}", userId, operationId, deliveryChannel);
        Optional<User> userOptional = userService.getUserById(userId);
        if (userOptional.isEmpty()) {
            logger.error("User with ID {} not found", userId);
            throw new IllegalArgumentException("User not found");
        }
        OtpConfig config = otpConfigService.getConfig();
        String code = generateOtpCode(config.getLength());
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusNanos(config.getExpirationTimeMs() * 1_000_000L);
        OtpCode otpCode = new OtpCode(
                userId,
                operationId,
                code,
                OtpCode.Status.ACTIVE,
                deliveryChannel,
                expiresAt
        );
        OtpCode savedCode = otpCodeDao.save(otpCode);
        logger.info("OTP code saved with ID: {}", savedCode.getId());
        OtpDeliveryService deliveryService = OtpDeliveryServiceFactory.getInstance()
                .getDeliveryService(deliveryChannel);
        if (!deliveryService.canDeliver(recipient)) {
            logger.error("Cannot deliver OTP to recipient: {} via channel: {}", recipient, deliveryChannel);
            otpCodeDao.updateStatus(savedCode.getId(), OtpCode.Status.EXPIRED);
            throw new IllegalArgumentException("Cannot deliver OTP to this recipient via " + deliveryChannel);
        }
        boolean sent = deliveryService.sendOtp(recipient, code);
        if (!sent) {
            logger.error("Failed to send OTP code to recipient: {} via channel: {}", recipient, deliveryChannel);
            otpCodeDao.updateStatus(savedCode.getId(), OtpCode.Status.EXPIRED);
            throw new RuntimeException("Failed to send OTP code");
        }
        logger.info("OTP code successfully sent to recipient via {}", deliveryChannel);
        return savedCode;
    }
    /**
     * Проверяет валидность OTP кода
     * 
     * @param operationId ID операции
     * @param code OTP код
     * @return true если код валидный, false в противном случае
     */
    public boolean validateOtp(String operationId, String code) {
        logger.info("Validating OTP for operation: {}", operationId);
        Optional<OtpCode> otpCodeOptional = otpCodeDao.findByOperationIdAndCode(operationId, code);
        if (otpCodeOptional.isEmpty()) {
            logger.warn("OTP code not found for operation: {}", operationId);
            return false;
        }
        OtpCode otpCode = otpCodeOptional.get();
        if (otpCode.getStatus() != OtpCode.Status.ACTIVE) {
            logger.warn("OTP code for operation {} is not active, status: {}", operationId, otpCode.getStatus());
            return false;
        }
        if (otpCode.isExpired()) {
            logger.warn("OTP code for operation {} has expired", operationId);
            otpCodeDao.updateStatus(otpCode.getId(), OtpCode.Status.EXPIRED);
            return false;
        }
        otpCodeDao.updateStatus(otpCode.getId(), OtpCode.Status.USED);
        logger.info("OTP code for operation {} successfully validated", operationId);
        return true;
    }
    /**
     * Обновляет статусы просроченных OTP кодов
     */
    public void updateExpiredCodes() {
        logger.info("Updating expired OTP codes");
        otpCodeDao.updateExpiredStatuses();
    }
    /**
     * Удаляет все OTP коды, связанные с пользователем
     * 
     * @param userId ID пользователя
     */
    public void deleteOtpCodesByUserId(Long userId) {
        logger.info("Deleting all OTP codes for user ID: {}", userId);
        otpCodeDao.deleteByUserId(userId);
    }
    /**
     * Генерирует OTP код заданной длины
     * 
     * @param length Длина кода
     * @return Сгенерированный код
     */
    private String generateOtpCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        }
        return builder.toString();
    }
} 