package com.zncloud.user.admin.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum WebhookStatus {
    ACTIVE("ACTIVE", "正常"),
    DISABLED("DISABLED", "已禁用");

    @EnumValue
    private final String code;

    @JsonValue
    private final String description;

    WebhookStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
}
