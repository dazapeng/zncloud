package com.zncloud.user.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public class CreateWebhookRequest {

    @NotBlank(message = "Webhook 名称不能为空")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "回调URL不能为空")
    @Size(max = 500)
    private String url;

    @NotEmpty(message = "至少订阅一个事件")
    private List<String> events;

    @Size(max = 255)
    private String secret;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public List<String> getEvents() { return events; }
    public void setEvents(List<String> events) { this.events = events; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
}
