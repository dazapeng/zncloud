package com.zncloud.session.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("session_audit_log")
public class SessionAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话ID */
    private String sessionId;

    /** 事件类型: VIOLATION_DETECTED/SESSION_DISCONNECTED/REVIEW_ACTION */
    private String eventType;

    /** 风险等级 */
    private String riskLevel;

    /** 事件描述 */
    private String description;

    /** 操作人ID(人工审核时) */
    private Long operatorId;

    /** 操作人备注 */
    private String operatorComment;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
