package com.zncloud.user.admin.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum WebhookEventType {
    SESSION_STARTED("session.started", "会话开始"),
    SESSION_ENDED("session.ended", "会话结束"),
    DEVICE_ONLINE("device.online", "设备上线"),
    DEVICE_OFFLINE("device.offline", "设备离线"),
    BILLING_CHARGED("billing.charged", "计费扣款"),
    CAFE_REGISTERED("cafe.registered", "网吧注册");

    @EnumValue
    private final String code;

    @JsonValue
    private final String description;

    WebhookEventType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static WebhookEventType fromCode(String code) {
        for (WebhookEventType t : values()) {
            if (t.code.equals(code)) return t;
        }
        return null;
    }
}
