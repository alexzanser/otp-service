package com.otpservice.api.admin;
import com.otpservice.api.BaseHandler;
import com.otpservice.model.User;
import com.otpservice.service.OtpService;
import com.otpservice.service.UserService;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * Обработчик для удаления пользователя администратором
 */
public class DeleteUserHandler extends BaseHandler {
    private final UserService userService;
    private final OtpService otpService;
    private static final Pattern USER_ID_PATTERN = Pattern.compile("/admin/users/(\\d+)");
    public DeleteUserHandler() {
        this.userService = new UserService();
        this.otpService = new OtpService();
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
            sendErrorResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        if (!validateTokenAndRole(exchange, User.Role.ADMIN)) {
            return;
        }
        String path = exchange.getRequestURI().getPath();
        Matcher matcher = USER_ID_PATTERN.matcher(path);
        if (!matcher.matches()) {
            sendErrorResponse(exchange, 400, "Invalid URL format. Expected: /admin/users/{userId}");
            return;
        }
        Long userId;
        try {
            userId = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid user ID format");
            return;
        }
        try {
            otpService.deleteOtpCodesByUserId(userId);
            boolean deleted = userService.deleteUser(userId);
            if (!deleted) {
                sendErrorResponse(exchange, 404, "User not found");
                return;
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User and related OTP codes deleted successfully");
            sendJsonResponse(exchange, 200, response);
        } catch (IllegalArgumentException e) {
            sendErrorResponse(exchange, 400, e.getMessage());
        } catch (Exception e) {
            logger.error("Error deleting user", e);
            sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }
} 