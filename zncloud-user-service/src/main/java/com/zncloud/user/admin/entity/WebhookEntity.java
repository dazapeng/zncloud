package com.zncloud.user.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zncloud.user.admin.enums.WebhookStatus;
import java.time.LocalDateTime;

@TableName("webhooks")
public class WebhookEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String url;

    private String events;

    private String secret;

    private WebhookStatus status;

    private Integer retryCount;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getEvents() { return events; }
    public void setEvents(String events) { this.events = events; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public WebhookStatus getStatus() { return status; }
    public void setStatus(WebhookStatus status) { this.status = status; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
