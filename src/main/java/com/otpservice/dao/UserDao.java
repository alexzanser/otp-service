package com.otpservice.dao;
import com.otpservice.config.DatabaseConfig;
import com.otpservice.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
public class UserDao {
    private static final Logger logger = LoggerFactory.getLogger(UserDao.class);
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password, role, created_at FROM users WHERE username = ?";
        logger.debug("Executing SQL to find user by username: {}", username);
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            logger.debug("Got database connection, preparing statement");
            statement.setString(1, username);
            logger.debug("Executing query for username: {}", username);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                logger.debug("User found, mapping result set");
                User user = mapResultSetToUser(resultSet);
                return Optional.of(user);
            }
            logger.debug("No user found with username: {}", username);
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Error finding user by username: {}", e.getMessage(), e);
            throw new RuntimeException("Error finding user by username", e);
        }
    }
    public boolean existsByRole(User.Role role) {
        String sql = "SELECT COUNT(*) FROM users WHERE role = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, role.name());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                return count > 0;
            }
            return false;
        } catch (SQLException e) {
            logger.error("Error checking if role exists", e);
            throw new RuntimeException("Error checking if role exists", e);
        }
    }
    public User save(User user) {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?) RETURNING id, created_at";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPassword());
            statement.setString(3, user.getRole().name());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                user.setId(resultSet.getLong("id"));
                user.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
                return user;
            }
            throw new SQLException("Failed to insert user, no ID obtained.");
        } catch (SQLException e) {
            logger.error("Error saving user", e);
            throw new RuntimeException("Error saving user", e);
        }
    }
    public List<User> findAllNonAdminUsers() {
        String sql = "SELECT id, username, password, role, created_at FROM users WHERE role != 'ADMIN'";
        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            List<User> users = new ArrayList<>();
            while (resultSet.next()) {
                User user = mapResultSetToUser(resultSet);
                users.add(user);
            }
            return users;
        } catch (SQLException e) {
            logger.error("Error finding all non-admin users", e);
            throw new RuntimeException("Error finding all non-admin users", e);
        }
    }
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error deleting user", e);
            throw new RuntimeException("Error deleting user", e);
        }
    }
    public Optional<User> findById(Long id) {
        String sql = "SELECT id, username, password, role, created_at FROM users WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                User user = mapResultSetToUser(resultSet);
                return Optional.of(user);
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Error finding user by id", e);
            throw new RuntimeException("Error finding user by id", e);
        }
    }
    private User mapResultSetToUser(ResultSet resultSet) throws SQLException {
        Long id = resultSet.getLong("id");
        String username = resultSet.getString("username");
        String password = resultSet.getString("password");
        User.Role role = User.Role.valueOf(resultSet.getString("role"));
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        return new User(id, username, password, role, createdAt);
    }
} 