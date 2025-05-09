package com.otpservice.util;
import com.otpservice.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;
/**
 * Утилитарный класс для инициализации базы данных
 */
public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    private static final String INIT_SQL_FILE = "db/init.sql";
    /**
     * Инициализирует базу данных, выполняя SQL-скрипт
     * 
     * @return true, если инициализация прошла успешно, false в противном случае
     */
    public static boolean initializeDatabase() {
        logger.info("Initializing database...");
        try (Connection connection = DatabaseConfig.getConnection()) {
            String sqlScript = readSqlScript();
            if (sqlScript == null || sqlScript.trim().isEmpty()) {
                logger.error("SQL script is empty or not found");
                return false;
            }
            try (Statement statement = connection.createStatement()) {
                for (String sql : sqlScript.split(";")) {
                    if (!sql.trim().isEmpty()) {
                        statement.execute(sql);
                    }
                }
                logger.info("Database initialized successfully");
                return true;
            }
        } catch (SQLException e) {
            logger.error("Database initialization failed", e);
            return false;
        } finally {
            DatabaseConfig.releaseConnection(null);
        }
    }
    /**
     * Читает SQL-скрипт из ресурсов
     * 
     * @return Содержимое SQL-скрипта или null, если скрипт не найден
     */
    private static String readSqlScript() {
        try (InputStream inputStream = DatabaseInitializer.class.getClassLoader().getResourceAsStream(INIT_SQL_FILE)) {
            if (inputStream == null) {
                logger.error("SQL script file not found: {}", INIT_SQL_FILE);
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            logger.error("Error reading SQL script", e);
            return null;
        }
    }
} 