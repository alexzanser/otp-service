package com.otpservice.api;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.otpservice.model.User;
import com.otpservice.util.JwtUtil;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
/**
 * Базовый класс для HTTP-обработчиков
 */
public abstract class BaseHandler implements HttpHandler {
    protected static final Logger logger = LoggerFactory.getLogger(BaseHandler.class);
    protected static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    /**
     * Отправляет JSON-ответ
     * 
     * @param exchange HTTP-обмен
     * @param statusCode Код статуса HTTP
     * @param responseObject Объект ответа, который будет сериализован в JSON
     * @throws IOException если произошла ошибка ввода-вывода
     */
    protected void sendJsonResponse(HttpExchange exchange, int statusCode, Object responseObject) throws IOException {
        String jsonResponse = objectMapper.writeValueAsString(responseObject);
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
    /**
     * Отправляет ответ с ошибкой
     * 
     * @param exchange HTTP-обмен
     * @param statusCode Код статуса HTTP
     * @param errorMessage Сообщение об ошибке
     * @throws IOException если произошла ошибка ввода-вывода
     */
    protected void sendErrorResponse(HttpExchange exchange, int statusCode, String errorMessage) throws IOException {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", errorMessage);
        sendJsonResponse(exchange, statusCode, errorResponse);
    }
    /**
     * Извлекает JWT-токен из заголовка Authorization
     * 
     * @param exchange HTTP-обмен
     * @return JWT-токен или null, если токен не найден или неверен
     */
    protected String extractJwtToken(HttpExchange exchange) {
        Headers headers = exchange.getRequestHeaders();
        if (!headers.containsKey(AUTHORIZATION_HEADER)) {
            return null;
        }
        String authHeader = headers.getFirst(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authHeader.substring(BEARER_PREFIX.length());
    }
    /**
     * Проверяет JWT-токен и роль пользователя
     * 
     * @param exchange HTTP-обмен
     * @param requiredRole Требуемая роль или null, если роль не важна
     * @return true, если токен валидный и роль соответствует требуемой (если указана)
     * @throws IOException если произошла ошибка ввода-вывода
     */
    protected boolean validateTokenAndRole(HttpExchange exchange, User.Role requiredRole) throws IOException {
        String token = extractJwtToken(exchange);
        if (token == null) {
            sendErrorResponse(exchange, 401, "Unauthorized: No token provided");
            return false;
        }
        if (!JwtUtil.validateToken(token)) {
            sendErrorResponse(exchange, 401, "Unauthorized: Invalid or expired token");
            return false;
        }
        if (requiredRole != null) {
            String role = JwtUtil.extractRole(token);
            if (role == null || !role.equals(requiredRole.name())) {
                sendErrorResponse(exchange, 403, "Forbidden: Insufficient permissions");
                return false;
            }
        }
        return true;
    }
    /**
     * Извлекает имя пользователя из JWT-токена
     * 
     * @param exchange HTTP-обмен
     * @return Имя пользователя или null, если токен не найден или неверен
     */
    protected String extractUsername(HttpExchange exchange) {
        String token = extractJwtToken(exchange);
        if (token == null || !JwtUtil.validateToken(token)) {
            return null;
        }
        return JwtUtil.extractUsername(token);
    }
    /**
     * Читает тело запроса как строку
     * 
     * @param exchange HTTP-обмен
     * @return Тело запроса в виде строки
     * @throws IOException если произошла ошибка ввода-вывода
     */
    protected String readRequestBody(HttpExchange exchange) throws IOException {
        StringBuilder content = new StringBuilder();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = exchange.getRequestBody().read(buffer)) != -1) {
            content.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
        }
        return content.toString();
    }
} 