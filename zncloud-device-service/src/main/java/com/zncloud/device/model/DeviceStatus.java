package com.zncloud.device.model;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DeviceStatus {

    REGISTERED("REGISTERED", "已注册"),
    ONLINE("ONLINE", "在线"),
    IN_USE("IN_USE", "使用中"),
    OFFLINE("OFFLINE", "离线"),
    PENDING_ONLINE("PENDING_ONLINE", "唤醒中");

    @EnumValue
    private final String value;

    @JsonValue
    private final String label;

    DeviceStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static DeviceStatus fromValue(String value) {
        for (DeviceStatus status : DeviceStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown device status: " + value);
    }
}
