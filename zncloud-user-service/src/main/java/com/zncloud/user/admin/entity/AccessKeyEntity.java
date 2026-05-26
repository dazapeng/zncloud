package com.zncloud.user.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zncloud.user.admin.enums.AccessKeyStatus;
import java.time.LocalDateTime;

@TableName("access_keys")
public class AccessKeyEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String keyId;

    private String keySecret;

    private String name;

    private AccessKeyStatus status;

    private String permissions;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime expiredAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }
    public String getKeySecret() { return keySecret; }
    public void setKeySecret(String keySecret) { this.keySecret = keySecret; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public AccessKeyStatus getStatus() { return status; }
    public void setStatus(AccessKeyStatus status) { this.status = status; }
    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
