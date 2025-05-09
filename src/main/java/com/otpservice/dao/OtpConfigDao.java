package com.otpservice.dao;
import com.otpservice.config.DatabaseConfig;
import com.otpservice.model.OtpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
public class OtpConfigDao {
    private static final Logger logger = LoggerFactory.getLogger(OtpConfigDao.class);
    private static final int CONFIG_ID = 1;
    public Optional<OtpConfig> getConfig() {
        String sql = "SELECT id, length, expiration_time_ms, updated_at FROM otp_config WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, CONFIG_ID);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                OtpConfig otpConfig = mapResultSetToOtpConfig(resultSet);
                return Optional.of(otpConfig);
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Error getting OTP config", e);
            throw new RuntimeException("Error getting OTP config", e);
        }
    }
    public void updateConfig(OtpConfig otpConfig) {
        String sql = "UPDATE otp_config SET length = ?, expiration_time_ms = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, otpConfig.getLength());
            statement.setInt(2, otpConfig.getExpirationTimeMs());
            statement.setInt(3, CONFIG_ID);
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                insertDefaultConfig(connection, otpConfig);
            }
        } catch (SQLException e) {
            logger.error("Error updating OTP config", e);
            throw new RuntimeException("Error updating OTP config", e);
        }
    }
    private void insertDefaultConfig(Connection connection, OtpConfig otpConfig) throws SQLException {
        String insertSql = "INSERT INTO otp_config (id, length, expiration_time_ms) VALUES (?, ?, ?)";
        try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
            insertStatement.setInt(1, CONFIG_ID);
            insertStatement.setInt(2, otpConfig.getLength());
            insertStatement.setInt(3, otpConfig.getExpirationTimeMs());
            insertStatement.executeUpdate();
        }
    }
    private OtpConfig mapResultSetToOtpConfig(ResultSet resultSet) throws SQLException {
        Integer id = resultSet.getInt("id");
        Integer length = resultSet.getInt("length");
        Integer expirationTimeMs = resultSet.getInt("expiration_time_ms");
        LocalDateTime updatedAt = resultSet.getTimestamp("updated_at").toLocalDateTime();
        return new OtpConfig(id, length, expirationTimeMs, updatedAt);
    }
}