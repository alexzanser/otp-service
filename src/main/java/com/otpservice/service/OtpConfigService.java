package com.otpservice.service;
import com.otpservice.config.AppConfig;
import com.otpservice.dao.OtpConfigDao;
import com.otpservice.model.OtpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;
public class OtpConfigService {
    private static final Logger logger = LoggerFactory.getLogger(OtpConfigService.class);
    private final OtpConfigDao otpConfigDao;
    public OtpConfigService() {
        this.otpConfigDao = new OtpConfigDao();
    }
    public OtpConfig getConfig() {
        logger.debug("Getting OTP configuration");
        Optional<OtpConfig> otpConfigOptional = otpConfigDao.getConfig();
        if (otpConfigOptional.isPresent()) {
            return otpConfigOptional.get();
        } else {
            logger.info("Creating default OTP configuration");
            OtpConfig defaultConfig = new OtpConfig(
                    AppConfig.getDefaultOtpLength(),
                    AppConfig.getDefaultOtpExpirationMs()
            );
            updateConfig(defaultConfig);
            return defaultConfig;
        }
    }
    public void updateConfig(OtpConfig otpConfig) {
        logger.info("Updating OTP configuration: length={}, expirationTimeMs={}", 
                    otpConfig.getLength(), otpConfig.getExpirationTimeMs());
        if (otpConfig.getLength() < 4 || otpConfig.getLength() > 10) {
            logger.warn("Invalid OTP length: {}", otpConfig.getLength());
            throw new IllegalArgumentException("OTP length must be between 4 and 10");
        }
        if (otpConfig.getExpirationTimeMs() < 60000 || otpConfig.getExpirationTimeMs() > 3600000) {
            logger.warn("Invalid OTP expiration time: {}", otpConfig.getExpirationTimeMs());
            throw new IllegalArgumentException("OTP expiration time must be between 1 minute and 1 hour");
        }
        otpConfigDao.updateConfig(otpConfig);
        logger.info("OTP configuration updated successfully");
    }
} 