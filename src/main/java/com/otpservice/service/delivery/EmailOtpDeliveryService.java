package com.otpservice.service.delivery;
import com.otpservice.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.regex.Pattern;
/**
 * Сервис для отправки OTP-кодов по электронной почте
 */
public class EmailOtpDeliveryService implements OtpDeliveryService {
    private static final Logger logger = LoggerFactory.getLogger(EmailOtpDeliveryService.class);
    private static final String EMAIL_SUBJECT = "Your OTP Code";
    private static final String EMAIL_TEMPLATE = "Your one-time password (OTP) code is: %s\n\nThis code will expire soon.";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );
    private final Session session;
    private final String sender;
    public EmailOtpDeliveryService() {
        Properties props = new Properties();
        props.put("mail.smtp.host", AppConfig.getMailSmtpHost());
        props.put("mail.smtp.port", AppConfig.getMailSmtpPort());
        props.put("mail.smtp.auth", AppConfig.getMailSmtpAuth());
        props.put("mail.smtp.starttls.enable", AppConfig.getMailSmtpStartTlsEnable());
        sender = AppConfig.getMailUsername();
        final String password = AppConfig.getMailPassword();
        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(sender, password);
            }
        });
        logger.info("Email OTP delivery service initialized with sender: {}", sender);
    }
    @Override
    public boolean sendOtp(String recipient, String code) {
        logger.info("Sending OTP code to email: {}", recipient);
        if (!isValidEmail(recipient)) {
            logger.warn("Invalid email address: {}", recipient);
            return false;
        }
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(sender));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            message.setSubject(EMAIL_SUBJECT);
            message.setText(String.format(EMAIL_TEMPLATE, code));
            Transport.send(message);
            logger.info("OTP code successfully sent to email: {}", recipient);
            return true;
        } catch (MessagingException e) {
            logger.error("Error sending OTP code to email: {}", recipient, e);
            return false;
        }
    }
    @Override
    public boolean canDeliver(String recipient) {
        return isValidEmail(recipient);
    }
    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
} 