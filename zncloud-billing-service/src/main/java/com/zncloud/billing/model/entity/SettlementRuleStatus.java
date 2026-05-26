package com.zncloud.billing.model.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SettlementRuleStatus {

    ENABLED("ENABLED", "启用"),
    DISABLED("DISABLED", "停用");

    @EnumValue
    private final String value;

    @JsonValue
    private final String label;

    SettlementRuleStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static SettlementRuleStatus fromValue(String value) {
        for (SettlementRuleStatus status : SettlementRuleStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown settlement rule status: " + value);
    }
}
