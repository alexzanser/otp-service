package com.otpservice;
import com.otpservice.api.HttpServer;
import com.otpservice.config.DatabaseConfig;
import com.otpservice.service.OtpExpirationScheduler;
import com.otpservice.service.OtpService;
import com.otpservice.service.delivery.OtpDeliveryServiceFactory;
import com.otpservice.util.DatabaseInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
/**
 * Главный класс приложения
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        logger.info("Starting OTP Service application");
        try {
            if (!DatabaseInitializer.initializeDatabase()) {
                logger.error("Failed to initialize database, exiting application");
                System.exit(1);
            }
            OtpService otpService = new OtpService();
            OtpExpirationScheduler scheduler = new OtpExpirationScheduler(otpService);
            scheduler.start();
            HttpServer httpServer = new HttpServer();
            httpServer.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down application...");
                scheduler.stop();
                httpServer.stop();
                DatabaseConfig.closeAllConnections();
                OtpDeliveryServiceFactory.getInstance().shutdownAll();
                logger.info("Application shutdown completed");
            }));
            logger.info("OTP Service application started successfully");
            logger.info("Server is running at http://{}:{}/", httpServer.getHost(), httpServer.getPort());
        } catch (IOException e) {
            logger.error("Error starting application", e);
            System.exit(1);
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            System.exit(1);
        }
    }
} 