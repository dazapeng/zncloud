package com.zncloud.billing.model.enums;

public enum BillingRateStatus {
    ACTIVE("生效中"),
    INACTIVE("已停用"),
    HISTORY("历史记录");

    private final String description;

    BillingRateStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
