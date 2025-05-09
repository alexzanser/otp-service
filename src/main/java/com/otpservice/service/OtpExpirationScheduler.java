package com.otpservice.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
/**
 * Планировщик для обновления статусов просроченных OTP кодов
 */
public class OtpExpirationScheduler {
    private static final Logger logger = LoggerFactory.getLogger(OtpExpirationScheduler.class);
    private static final int DEFAULT_INITIAL_DELAY_SECONDS = 30;
    private static final int DEFAULT_PERIOD_SECONDS = 60;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final OtpService otpService;
    public OtpExpirationScheduler(OtpService otpService) {
        this.otpService = otpService;
    }
    /**
     * Запускает планировщик с интервалом по умолчанию
     */
    public void start() {
        start(DEFAULT_INITIAL_DELAY_SECONDS, DEFAULT_PERIOD_SECONDS);
    }
    /**
     * Запускает планировщик с заданным интервалом
     * 
     * @param initialDelaySeconds Начальная задержка в секундах
     * @param periodSeconds Период в секундах
     */
    public void start(int initialDelaySeconds, int periodSeconds) {
        logger.info("Starting OTP expiration scheduler with period {} seconds", periodSeconds);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                otpService.updateExpiredCodes();
            } catch (Exception e) {
                logger.error("Error updating expired OTP codes", e);
            }
        }, initialDelaySeconds, periodSeconds, TimeUnit.SECONDS);
    }
    /**
     * Останавливает планировщик
     */
    public void stop() {
        logger.info("Stopping OTP expiration scheduler");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("Scheduler did not terminate in time");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("Scheduler shutdown interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
} 