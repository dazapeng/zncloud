package com.zncloud.device.model;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CafeStatus {

    ACTIVE("ACTIVE", "营业中"),
    INACTIVE("INACTIVE", "已停业"),
    SUSPENDED("SUSPENDED", "暂停营业");

    @EnumValue
    private final String value;

    @JsonValue
    private final String label;

    CafeStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static CafeStatus fromValue(String value) {
        for (CafeStatus status : CafeStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown cafe status: " + value);
    }
}
