package com.zncloud.user.model.dto;

import com.zncloud.user.model.entity.User;
import com.zncloud.user.model.enums.UserRole;
import com.zncloud.user.model.enums.UserStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UserProfileResponse {

    private Long id;
    private String nickname;
    private String phone;
    private String avatar;
    private UserRole role;
    private BigDecimal balance;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public static UserProfileResponse fromUser(User user) {
        UserProfileResponse resp = new UserProfileResponse();
        resp.setId(user.getId());
        resp.setNickname(user.getNickname());
        resp.setPhone(user.getPhone());
        resp.setAvatar(user.getAvatar());
        resp.setRole(user.getRole());
        resp.setBalance(user.getBalance());
        resp.setStatus(user.getStatus());
        resp.setCreatedAt(user.getCreatedAt());
        resp.setLastLoginAt(user.getLastLoginAt());
        return resp;
    }

    // ===== Getters & Setters =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
