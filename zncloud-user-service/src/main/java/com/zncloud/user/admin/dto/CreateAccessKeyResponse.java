package com.zncloud.user.admin.dto;

import com.zncloud.user.admin.entity.AccessKeyEntity;
import java.time.LocalDateTime;

public class CreateAccessKeyResponse {

    private Long id;
    private String keyId;
    private String keySecret;
    private String name;
    private LocalDateTime createdAt;

    public static CreateAccessKeyResponse fromEntity(AccessKeyEntity entity, String plainSecret) {
        CreateAccessKeyResponse resp = new CreateAccessKeyResponse();
        resp.id = entity.getId();
        resp.keyId = entity.getKeyId();
        resp.keySecret = plainSecret;
        resp.name = entity.getName();
        resp.createdAt = entity.getCreatedAt();
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }
    public String getKeySecret() { return keySecret; }
    public void setKeySecret(String keySecret) { this.keySecret = keySecret; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
