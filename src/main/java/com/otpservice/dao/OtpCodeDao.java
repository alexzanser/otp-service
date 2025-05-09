package com.otpservice.dao;
import com.otpservice.config.DatabaseConfig;
import com.otpservice.model.OtpCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
public class OtpCodeDao {
    private static final Logger logger = LoggerFactory.getLogger(OtpCodeDao.class);
    public OtpCode save(OtpCode otpCode) {
        String sql = "INSERT INTO otp_codes (user_id, operation_id, code, status, delivery_channel, expires_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?) RETURNING id, created_at";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, otpCode.getUserId());
            statement.setString(2, otpCode.getOperationId());
            statement.setString(3, otpCode.getCode());
            statement.setString(4, otpCode.getStatus().name());
            statement.setString(5, otpCode.getDeliveryChannel().name());
            statement.setTimestamp(6, Timestamp.valueOf(otpCode.getExpiresAt()));
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                otpCode.setId(resultSet.getLong("id"));
                otpCode.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
                return otpCode;
            }
            throw new SQLException("Failed to insert OTP code, no ID obtained.");
        } catch (SQLException e) {
            logger.error("Error saving OTP code", e);
            throw new RuntimeException("Error saving OTP code", e);
        }
    }
    public Optional<OtpCode> findByOperationIdAndCode(String operationId, String code) {
        String sql = "SELECT id, user_id, operation_id, code, status, delivery_channel, created_at, expires_at " +
                     "FROM otp_codes WHERE operation_id = ? AND code = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, operationId);
            statement.setString(2, code);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                OtpCode otpCode = mapResultSetToOtpCode(resultSet);
                return Optional.of(otpCode);
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Error finding OTP code by operation ID and code", e);
            throw new RuntimeException("Error finding OTP code by operation ID and code", e);
        }
    }
    public void updateStatus(Long otpCodeId, OtpCode.Status status) {
        String sql = "UPDATE otp_codes SET status = ? WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setLong(2, otpCodeId);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating OTP code status", e);
            throw new RuntimeException("Error updating OTP code status", e);
        }
    }
    public void updateExpiredStatuses() {
        String sql = "UPDATE otp_codes SET status = ? " +
                     "WHERE status = ? AND expires_at < CURRENT_TIMESTAMP";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, OtpCode.Status.EXPIRED.name());
            statement.setString(2, OtpCode.Status.ACTIVE.name());
            int updatedRows = statement.executeUpdate();
            if (updatedRows > 0) {
                logger.info("Updated {} expired OTP codes", updatedRows);
            }
        } catch (SQLException e) {
            logger.error("Error updating expired OTP codes", e);
            throw new RuntimeException("Error updating expired OTP codes", e);
        }
    }
    public List<OtpCode> findAllByUserId(Long userId) {
        String sql = "SELECT id, user_id, operation_id, code, status, delivery_channel, created_at, expires_at " +
                     "FROM otp_codes WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            ResultSet resultSet = statement.executeQuery();
            List<OtpCode> otpCodes = new ArrayList<>();
            while (resultSet.next()) {
                OtpCode otpCode = mapResultSetToOtpCode(resultSet);
                otpCodes.add(otpCode);
            }
            return otpCodes;
        } catch (SQLException e) {
            logger.error("Error finding all OTP codes by user ID", e);
            throw new RuntimeException("Error finding all OTP codes by user ID", e);
        }
    }
    public void deleteByUserId(Long userId) {
        String sql = "DELETE FROM otp_codes WHERE user_id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            int deletedRows = statement.executeUpdate();
            if (deletedRows > 0) {
                logger.info("Deleted {} OTP codes for user ID {}", deletedRows, userId);
            }
        } catch (SQLException e) {
            logger.error("Error deleting OTP codes by user ID", e);
            throw new RuntimeException("Error deleting OTP codes by user ID", e);
        }
    }
    private OtpCode mapResultSetToOtpCode(ResultSet resultSet) throws SQLException {
        Long id = resultSet.getLong("id");
        Long userId = resultSet.getLong("user_id");
        String operationId = resultSet.getString("operation_id");
        String code = resultSet.getString("code");
        OtpCode.Status status = OtpCode.Status.valueOf(resultSet.getString("status"));
        OtpCode.DeliveryChannel deliveryChannel = OtpCode.DeliveryChannel.valueOf(resultSet.getString("delivery_channel"));
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        LocalDateTime expiresAt = resultSet.getTimestamp("expires_at").toLocalDateTime();
        return new OtpCode(id, userId, operationId, code, status, deliveryChannel, createdAt, expiresAt);
    }
} 