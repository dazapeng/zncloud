package com.zncloud.user.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class WebhookEventRequest {

    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    @NotNull(message = "事件数据不能为空")
    private Map<String, Object> data;

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}
