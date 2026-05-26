package com.zncloud.user.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UserStatus {

    ACTIVE("ACTIVE", "正常"),
    BANNED("BANNED", "封禁"),
    LOCKED("LOCKED", "锁定");

    @EnumValue
    private final String code;

    @JsonValue
    private final String description;

    UserStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static UserStatus fromCode(String code) {
        for (UserStatus status : UserStatus.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return UserStatus.ACTIVE;
    }
}
