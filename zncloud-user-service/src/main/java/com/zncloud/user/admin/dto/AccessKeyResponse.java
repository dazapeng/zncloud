package com.zncloud.user.admin.dto;

import com.zncloud.user.admin.entity.AccessKeyEntity;
import com.zncloud.user.admin.enums.AccessKeyStatus;
import java.time.LocalDateTime;

public class AccessKeyResponse {

    private Long id;
    private String keyId;
    private String name;
    private AccessKeyStatus status;
    private String permissions;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;

    public static AccessKeyResponse fromEntity(AccessKeyEntity entity, boolean includeSecret) {
        AccessKeyResponse resp = new AccessKeyResponse();
        resp.id = entity.getId();
        resp.keyId = entity.getKeyId();
        resp.name = entity.getName();
        resp.status = entity.getStatus();
        resp.permissions = entity.getPermissions();
        resp.createdAt = entity.getCreatedAt();
        resp.expiredAt = entity.getExpiredAt();
        return resp;
    }

    public static AccessKeyResponse fromEntityWithSecret(AccessKeyEntity entity, String plainSecret) {
        AccessKeyResponse resp = fromEntity(entity, true);
        return resp;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public AccessKeyStatus getStatus() { return status; }
    public void setStatus(AccessKeyStatus status) { this.status = status; }
    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; }
}
