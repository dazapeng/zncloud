package com.zncloud.user.admin.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DeliveryStatus {
    PENDING("PENDING", "待投递"),
    SUCCESS("SUCCESS", "投递成功"),
    FAILED("FAILED", "投递失败");

    @EnumValue
    private final String code;

    @JsonValue
    private final String description;

    DeliveryStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
}
