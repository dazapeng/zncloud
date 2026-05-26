package com.zncloud.device.model;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Isp {

    CHINA_TELECOM("CHINA_TELECOM", "电信"),
    CHINA_UNICOM("CHINA_UNICOM", "联通"),
    CHINA_MOBILE("CHINA_MOBILE", "移动"),
    MULTI_LINE("MULTI_LINE", "多线");

    @EnumValue
    private final String value;

    @JsonValue
    private final String label;

    Isp(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static Isp fromValue(String value) {
        for (Isp isp : Isp.values()) {
            if (isp.value.equals(value)) {
                return isp;
            }
        }
        throw new IllegalArgumentException("Unknown ISP: " + value);
    }
}
