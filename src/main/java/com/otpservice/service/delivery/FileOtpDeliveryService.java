package com.otpservice.service.delivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
/**
 * Сервис для сохранения OTP-кодов в файл
 */
public class FileOtpDeliveryService implements OtpDeliveryService {
    private static final Logger logger = LoggerFactory.getLogger(FileOtpDeliveryService.class);
    private static final String OTP_FILES_DIR = "otp_files";
    private static final String OTP_FILE_NAME = "otp_codes.txt";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Override
    public boolean sendOtp(String recipient, String code) {
        logger.info("Saving OTP code for recipient: {}", recipient);
        String fileName = OTP_FILES_DIR + "/" + (recipient != null && !recipient.isEmpty() ? 
                recipient.replace("@", "_otp_code_") : OTP_FILE_NAME);
        try {
            Files.createDirectories(Paths.get(OTP_FILES_DIR));
            File file = new File(fileName);
            Path parentPath = file.toPath().getParent();
            if (parentPath != null) {
                Files.createDirectories(parentPath);
            }
            try (FileWriter writer = new FileWriter(file, true)) {
                String timestamp = LocalDateTime.now().format(formatter);
                writer.write(String.format("[%s] OTP code: %s\n", timestamp, code));
                logger.info("OTP code successfully saved to file: {}", file.getAbsolutePath());
                return true;
            }
        } catch (IOException e) {
            logger.error("Error saving OTP code to file", e);
            return false;
        }
    }
    @Override
    public boolean canDeliver(String recipient) {
        String fileName = OTP_FILES_DIR + "/" + (recipient != null && !recipient.isEmpty() ? 
                recipient.replace("@", "_otp_code_") : OTP_FILE_NAME);
        try {
            Path dirPath = Paths.get(OTP_FILES_DIR);
            if (!Files.exists(dirPath)) {
                try {
                    Files.createDirectories(dirPath);
                } catch (IOException e) {
                    logger.warn("Cannot create otp_files directory", e);
                    return false;
                }
            }
            if (!Files.isWritable(dirPath)) {
                logger.warn("Directory {} is not writable", OTP_FILES_DIR);
                return false;
            }
            Path filePath = Paths.get(fileName);
            if (Files.exists(filePath)) {
                return Files.isWritable(filePath);
            }
            return true;
        } catch (Exception e) {
            logger.warn("Cannot check if file is writable: {}", fileName, e);
            return false;
        }
    }
} 