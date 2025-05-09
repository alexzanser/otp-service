package com.otpservice.model;
import java.time.LocalDateTime;
public class OtpCode {
    private Long id;
    private Long userId;
    private String operationId;
    private String code;
    private Status status;
    private DeliveryChannel deliveryChannel;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    public enum Status {
        ACTIVE, EXPIRED, USED
    }
    public enum DeliveryChannel {
        SMS, EMAIL, TELEGRAM, FILE
    }
    public OtpCode() {
    }
    public OtpCode(Long userId, String operationId, String code, Status status, 
                  DeliveryChannel deliveryChannel, LocalDateTime expiresAt) {
        this.userId = userId;
        this.operationId = operationId;
        this.code = code;
        this.status = status;
        this.deliveryChannel = deliveryChannel;
        this.expiresAt = expiresAt;
    }
    public OtpCode(Long id, Long userId, String operationId, String code, 
                  Status status, DeliveryChannel deliveryChannel, 
                  LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.userId = userId;
        this.operationId = operationId;
        this.code = code;
        this.status = status;
        this.deliveryChannel = deliveryChannel;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    public String getOperationId() {
        return operationId;
    }
    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }
    public DeliveryChannel getDeliveryChannel() {
        return deliveryChannel;
    }
    public void setDeliveryChannel(DeliveryChannel deliveryChannel) {
        this.deliveryChannel = deliveryChannel;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    @Override
    public String toString() {
        return "OtpCode{" +
                "id=" + id +
                ", userId=" + userId +
                ", operationId='" + operationId + '\'' +
                ", status=" + status +
                ", deliveryChannel=" + deliveryChannel +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                '}';
    }
} 