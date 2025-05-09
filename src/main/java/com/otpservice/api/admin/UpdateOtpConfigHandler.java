package com.otpservice.api.admin;
import com.fasterxml.jackson.databind.JsonNode;
import com.otpservice.api.BaseHandler;
import com.otpservice.model.OtpConfig;
import com.otpservice.model.User;
import com.otpservice.service.OtpConfigService;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
/**
 * Обработчик для обновления конфигурации OTP администратором
 */
public class UpdateOtpConfigHandler extends BaseHandler {
    private final OtpConfigService otpConfigService;
    public UpdateOtpConfigHandler() {
        this.otpConfigService = new OtpConfigService();
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
            sendErrorResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        if (!validateTokenAndRole(exchange, User.Role.ADMIN)) {
            return;
        }
        try {
            String requestBody = readRequestBody(exchange);
            JsonNode jsonNode = objectMapper.readTree(requestBody);
            Integer length = jsonNode.has("length") ? jsonNode.get("length").asInt() : null;
            Integer expirationTimeMs = jsonNode.has("expirationTimeMs") ? jsonNode.get("expirationTimeMs").asInt() : null;
            if (length == null || expirationTimeMs == null) {
                sendErrorResponse(exchange, 400, "Length and expirationTimeMs are required");
                return;
            }
            OtpConfig config = new OtpConfig(length, expirationTimeMs);
            otpConfigService.updateConfig(config);
            OtpConfig updatedConfig = otpConfigService.getConfig();
            Map<String, Object> response = new HashMap<>();
            response.put("length", updatedConfig.getLength());
            response.put("expirationTimeMs", updatedConfig.getExpirationTimeMs());
            response.put("updatedAt", updatedConfig.getUpdatedAt().toString());
            response.put("success", true);
            response.put("message", "OTP configuration updated successfully");
            sendJsonResponse(exchange, 200, response);
        } catch (IllegalArgumentException e) {
            sendErrorResponse(exchange, 400, e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating OTP configuration", e);
            sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }
} 