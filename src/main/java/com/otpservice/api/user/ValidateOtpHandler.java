package com.otpservice.api.user;
import com.fasterxml.jackson.databind.JsonNode;
import com.otpservice.api.BaseHandler;
import com.otpservice.service.OtpService;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
/**
 * Обработчик для валидации OTP кодов
 */
public class ValidateOtpHandler extends BaseHandler {
    private final OtpService otpService;
    public ValidateOtpHandler() {
        this.otpService = new OtpService();
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendErrorResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        if (!validateTokenAndRole(exchange, null)) {
            return;
        }
        try {
            String requestBody = readRequestBody(exchange);
            JsonNode jsonNode = objectMapper.readTree(requestBody);
            String operationId = jsonNode.has("operationId") ? jsonNode.get("operationId").asText() : null;
            String code = jsonNode.has("code") ? jsonNode.get("code").asText() : null;
            if (operationId == null || operationId.isEmpty()) {
                sendErrorResponse(exchange, 400, "Operation ID is required");
                return;
            }
            if (code == null || code.isEmpty()) {
                sendErrorResponse(exchange, 400, "OTP code is required");
                return;
            }
            boolean isValid = otpService.validateOtp(operationId, code);
            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);
            if (isValid) {
                response.put("message", "OTP code is valid");
            } else {
                response.put("message", "OTP code is invalid or expired");
            }
            sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            logger.error("Error validating OTP code", e);
            sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }
} 