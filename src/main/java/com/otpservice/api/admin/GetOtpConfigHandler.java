package com.otpservice.api.admin;
import com.otpservice.api.BaseHandler;
import com.otpservice.model.OtpConfig;
import com.otpservice.model.User;
import com.otpservice.service.OtpConfigService;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
/**
 * Обработчик для получения текущей конфигурации OTP администратором
 */
public class GetOtpConfigHandler extends BaseHandler {
    private final OtpConfigService otpConfigService;
    public GetOtpConfigHandler() {
        this.otpConfigService = new OtpConfigService();
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendErrorResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        if (!validateTokenAndRole(exchange, User.Role.ADMIN)) {
            return;
        }
        try {
            OtpConfig config = otpConfigService.getConfig();
            Map<String, Object> response = new HashMap<>();
            response.put("length", config.getLength());
            response.put("expirationTimeMs", config.getExpirationTimeMs());
            response.put("updatedAt", config.getUpdatedAt().toString());
            sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            logger.error("Error getting OTP configuration", e);
            sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }
} 