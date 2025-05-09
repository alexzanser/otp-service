package com.otpservice.api.auth;
import com.fasterxml.jackson.databind.JsonNode;
import com.otpservice.api.BaseHandler;
import com.otpservice.model.User;
import com.otpservice.service.UserService;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
/**
 * Обработчик для регистрации новых пользователей
 */
public class RegisterHandler extends BaseHandler {
    private final UserService userService;
    public RegisterHandler() {
        this.userService = new UserService();
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendErrorResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        try {
            String requestBody = readRequestBody(exchange);
            JsonNode jsonNode = objectMapper.readTree(requestBody);
            String username = jsonNode.has("username") ? jsonNode.get("username").asText() : null;
            String password = jsonNode.has("password") ? jsonNode.get("password").asText() : null;
            String roleStr = jsonNode.has("role") ? jsonNode.get("role").asText() : "USER";
            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                sendErrorResponse(exchange, 400, "Username and password are required");
                return;
            }
            User.Role role;
            try {
                role = User.Role.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                sendErrorResponse(exchange, 400, "Invalid role: " + roleStr);
                return;
            }
            User user = userService.register(username, password, role);
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("role", user.getRole().name());
            response.put("created_at", user.getCreatedAt().toString());
            response.put("success", true);
            response.put("message", "User registered successfully");
            sendJsonResponse(exchange, 201, response);
        } catch (IllegalArgumentException e) {
            sendErrorResponse(exchange, 400, e.getMessage());
        } catch (Exception e) {
            logger.error("Error registering user", e);
            sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }
} 