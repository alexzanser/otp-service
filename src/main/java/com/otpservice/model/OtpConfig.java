package com.otpservice.model;
import java.time.LocalDateTime;
public class OtpConfig {
    private Integer id;
    private Integer length;
    private Integer expirationTimeMs;
    private LocalDateTime updatedAt;
    public OtpConfig() {
    }
    public OtpConfig(Integer length, Integer expirationTimeMs) {
        this.id = 1;
        this.length = length;
        this.expirationTimeMs = expirationTimeMs;
    }
    public OtpConfig(Integer id, Integer length, Integer expirationTimeMs, LocalDateTime updatedAt) {
        this.id = id;
        this.length = length;
        this.expirationTimeMs = expirationTimeMs;
        this.updatedAt = updatedAt;
    }
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public Integer getLength() {
        return length;
    }
    public void setLength(Integer length) {
        this.length = length;
    }
    public Integer getExpirationTimeMs() {
        return expirationTimeMs;
    }
    public void setExpirationTimeMs(Integer expirationTimeMs) {
        this.expirationTimeMs = expirationTimeMs;
    }
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    @Override
    public String toString() {
        return "OtpConfig{" +
                "id=" + id +
                ", length=" + length +
                ", expirationTimeMs=" + expirationTimeMs +
                ", updatedAt=" + updatedAt +
                '}';
    }
} 