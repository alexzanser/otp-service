package com.otpservice.api.admin;
import com.otpservice.api.BaseHandler;
import com.otpservice.model.User;
import com.otpservice.service.UserService;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Обработчик для получения списка всех пользователей (кроме админов) администратором
 */
public class GetAllUsersHandler extends BaseHandler {
    private final UserService userService;
    public GetAllUsersHandler() {
        this.userService = new UserService();
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
            List<User> users = userService.getAllNonAdminUsers();
            List<Map<String, Object>> usersList = new ArrayList<>();
            for (User user : users) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("role", user.getRole().name());
                userMap.put("createdAt", user.getCreatedAt().toString());
                usersList.add(userMap);
            }
            Map<String, Object> response = new HashMap<>();
            response.put("users", usersList);
            response.put("count", usersList.size());
            sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            logger.error("Error getting users list", e);
            sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }
} 