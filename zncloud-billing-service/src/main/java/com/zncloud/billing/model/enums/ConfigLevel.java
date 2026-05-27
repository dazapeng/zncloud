package com.zncloud.billing.model.enums;

public enum ConfigLevel {
    ENTRY("入门"),
    MAINSTREAM("主流"),
    HIGH_PERFORMANCE("高性能");

    private final String description;

    ConfigLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
