package com.otpservice.api.user;
import com.fasterxml.jackson.databind.JsonNode;
import com.otpservice.api.BaseHandler;
import com.otpservice.model.OtpCode;
import com.otpservice.model.User;
import com.otpservice.service.OtpService;
import com.otpservice.service.UserService;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
/**
 * Обработчик для генерации OTP кодов
 */
public class GenerateOtpHandler extends BaseHandler {
    private final OtpService otpService;
    private final UserService userService;
    public GenerateOtpHandler() {
        this.otpService = new OtpService();
        this.userService = new UserService();
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
        String username = extractUsername(exchange);
        if (username == null) {
            sendErrorResponse(exchange, 401, "Invalid token");
            return;
        }
        Optional<User> userOptional = userService.getUserByUsername(username);
        if (userOptional.isEmpty()) {
            sendErrorResponse(exchange, 401, "User not found");
            return;
        }
        User user = userOptional.get();
        try {
            String requestBody = readRequestBody(exchange);
            JsonNode jsonNode = objectMapper.readTree(requestBody);
            String operationId = jsonNode.has("operationId") ? jsonNode.get("operationId").asText() : null;
            String recipient = jsonNode.has("recipient") ? jsonNode.get("recipient").asText() : null;
            String deliveryChannelStr = jsonNode.has("deliveryChannel") ? jsonNode.get("deliveryChannel").asText() : null;
            if (operationId == null || operationId.isEmpty()) {
                sendErrorResponse(exchange, 400, "Operation ID is required");
                return;
            }
            if (recipient == null || recipient.isEmpty()) {
                sendErrorResponse(exchange, 400, "Recipient is required");
                return;
            }
            if (deliveryChannelStr == null || deliveryChannelStr.isEmpty()) {
                sendErrorResponse(exchange, 400, "Delivery channel is required");
                return;
            }
            OtpCode.DeliveryChannel deliveryChannel;
            try {
                deliveryChannel = OtpCode.DeliveryChannel.valueOf(deliveryChannelStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                sendErrorResponse(exchange, 400, "Invalid delivery channel: " + deliveryChannelStr);
                return;
            }
            OtpCode otpCode = otpService.generateAndSendOtp(user.getId(), operationId, recipient, deliveryChannel);
            Map<String, Object> response = new HashMap<>();
            response.put("operationId", otpCode.getOperationId());
            response.put("expiresAt", otpCode.getExpiresAt().toString());
            response.put("deliveryChannel", otpCode.getDeliveryChannel().name());
            response.put("success", true);
            response.put("message", "OTP code generated and sent successfully");
            sendJsonResponse(exchange, 201, response);
        } catch (IllegalArgumentException e) {
            sendErrorResponse(exchange, 400, e.getMessage());
        } catch (Exception e) {
            logger.error("Error generating OTP code", e);
            sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }
} 