package com.zncloud.user.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UserRole {

    USER("USER", "普通用户"),
    CAFE_ADMIN("CAFE_ADMIN", "网吧管理员"),
    OPERATOR("OPERATOR", "平台运营"),
    SUPER_ADMIN("SUPER_ADMIN", "超级管理员");

    @EnumValue
    private final String code;

    @JsonValue
    private final String description;

    UserRole(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static UserRole fromCode(String code) {
        for (UserRole role : UserRole.values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return UserRole.USER;
    }
}
