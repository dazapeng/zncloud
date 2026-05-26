package com.zncloud.device.model;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ConfigLevel {

    ENTRY("ENTRY", "入门级"),
    MAINSTREAM("MAINSTREAM", "主流级"),
    HIGH_PERFORMANCE("HIGH_PERFORMANCE", "高性能级");

    @EnumValue
    private final String value;

    @JsonValue
    private final String label;

    ConfigLevel(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static ConfigLevel fromValue(String value) {
        for (ConfigLevel level : ConfigLevel.values()) {
            if (level.value.equals(value)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown config level: " + value);
    }
}
