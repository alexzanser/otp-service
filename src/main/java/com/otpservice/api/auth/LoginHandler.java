package com.otpservice.api.auth;
import com.fasterxml.jackson.databind.JsonNode;
import com.otpservice.api.BaseHandler;
import com.otpservice.service.UserService;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
/**
 * Обработчик для аутентификации пользователей
 */
public class LoginHandler extends BaseHandler {
    private final UserService userService;
    public LoginHandler() {
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
            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                sendErrorResponse(exchange, 400, "Username and password are required");
                return;
            }
            String token = userService.authenticate(username, password);
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("success", true);
            response.put("message", "Authentication successful");
            sendJsonResponse(exchange, 200, response);
        } catch (IllegalArgumentException e) {
            sendErrorResponse(exchange, 401, e.getMessage());
        } catch (Exception e) {
            logger.error("Error authenticating user", e);
            sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }
} 