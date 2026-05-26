package com.zncloud.user.admin.dto;

import com.zncloud.user.admin.entity.WebhookEntity;
import com.zncloud.user.admin.enums.WebhookStatus;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WebhookResponse {

    private Long id;
    private String name;
    private String url;
    private List<String> events;
    private WebhookStatus status;
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WebhookResponse fromEntity(WebhookEntity entity) {
        WebhookResponse resp = new WebhookResponse();
        resp.id = entity.getId();
        resp.name = entity.getName();
        resp.url = entity.getUrl();
        // Parse JSON array string to List
        String eventsStr = entity.getEvents();
        if (eventsStr != null) {
            eventsStr = eventsStr.replaceAll("[\\[\\]\" ]", "");
            resp.events = Arrays.asList(eventsStr.split(","));
        } else {
            resp.events = List.of();
        }
        resp.status = entity.getStatus();
        resp.retryCount = entity.getRetryCount();
        resp.createdAt = entity.getCreatedAt();
        resp.updatedAt = entity.getUpdatedAt();
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public List<String> getEvents() { return events; }
    public void setEvents(List<String> events) { this.events = events; }
    public WebhookStatus getStatus() { return status; }
    public void setStatus(WebhookStatus status) { this.status = status; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
