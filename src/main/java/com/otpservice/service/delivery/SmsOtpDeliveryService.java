package com.otpservice.service.delivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;
/**
 * Эмулятор сервиса для отправки OTP-кодов по SMS
 */
public class SmsOtpDeliveryService implements OtpDeliveryService {
    private static final Logger logger = LoggerFactory.getLogger(SmsOtpDeliveryService.class);
    private static final String SMS_TEMPLATE = "Your OTP code is: %s";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");
    @Override
    public boolean sendOtp(String recipient, String code) {
        logger.info("Emulating SMS delivery to phone: {}", recipient);
        if (!isValidPhoneNumber(recipient)) {
            logger.warn("Invalid phone number: {}", recipient);
            return false;
        }
        String smsText = String.format(SMS_TEMPLATE, code);
        System.out.println("====== SMS EMULATOR ======");
        System.out.println("TO: " + recipient);
        System.out.println("MESSAGE: " + smsText);
        System.out.println("==========================");
        logger.info("SMS sent to {}: {}", recipient, smsText);
        return true;
    }
    @Override
    public boolean canDeliver(String recipient) {
        return isValidPhoneNumber(recipient);
    }
    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && PHONE_PATTERN.matcher(phoneNumber).matches();
    }
} 