package com.otpservice.api;
import com.otpservice.api.admin.DeleteUserHandler;
import com.otpservice.api.admin.GetAllUsersHandler;
import com.otpservice.api.admin.GetOtpConfigHandler;
import com.otpservice.api.admin.UpdateOtpConfigHandler;
import com.otpservice.api.auth.LoginHandler;
import com.otpservice.api.auth.RegisterHandler;
import com.otpservice.api.user.GenerateOtpHandler;
import com.otpservice.api.user.ValidateOtpHandler;
import com.otpservice.config.AppConfig;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
/**
 * HTTP-сервер приложения
 */
public class HttpServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
    private final String host;
    private final int port;
    private com.sun.net.httpserver.HttpServer server;
    public HttpServer() {
        this.host = AppConfig.getServerHost();
        this.port = AppConfig.getServerPort();
    }
    /**
     * Запускает HTTP-сервер и регистрирует обработчики
     * 
     * @throws IOException если произошла ошибка при запуске сервера
     */
    public void start() throws IOException {
        logger.info("Starting HTTP server on {} :{}", host, port);
        server = com.sun.net.httpserver.HttpServer.create(
                new InetSocketAddress(port),
                0
        );
        server.setExecutor(Executors.newFixedThreadPool(10));
        registerHandlers();
        server.start();
        logger.info("HTTP server started successfully");
    }
    /**
     * Останавливает HTTP-сервер
     */
    public void stop() {
        if (server != null) {
            logger.info("Stopping HTTP server");
            server.stop(3);
            logger.info("HTTP server stopped");
        }
    }
    /**
     * @return Хост, на котором запущен сервер
     */
    public String getHost() {
        return host;
    }
    /**
     * @return Порт, на котором запущен сервер
     */
    public int getPort() {
        return port;
    }
    /**
     * Регистрирует все обработчики API
     */
    private void registerHandlers() {
        createContext("/auth/register", new RegisterHandler());
        createContext("/auth/login", new LoginHandler());
        createContext("/admin/config", new GetOtpConfigHandler());
        createContext("/admin/config/update", new UpdateOtpConfigHandler());
        createContext("/admin/users", new GetAllUsersHandler());
        createContext("/admin/users/", new DeleteUserHandler());
        createContext("/otp/generate", new GenerateOtpHandler());
        createContext("/otp/validate", new ValidateOtpHandler());
    }
    /**
     * Создает контекст (привязывает путь к обработчику)
     * 
     * @param path Путь API
     * @param handler Обработчик запросов
     * @return Созданный контекст
     */
    private HttpContext createContext(String path, HttpHandler handler) {
        HttpContext context = server.createContext(path, handler);
        logger.debug("Registered handler for path: {}", path);
        return context;
    }
} 