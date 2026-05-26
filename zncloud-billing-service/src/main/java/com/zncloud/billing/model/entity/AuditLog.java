package com.zncloud.billing.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_log")
public class AuditLog {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 操作类型 (LOGIN, LOGOUT, CREATE_ORDER, etc.) */
    private String action;

    /** 目标对象类型 */
    private String targetType;

    /** 目标对象ID */
    private String targetId;

    /** 操作详情 (JSON) */
    private String detail;

    /** 请求IP */
    private String ipAddress;

    /** User-Agent */
    private String userAgent;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
