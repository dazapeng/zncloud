package com.zncloud.session.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("content_check_log")
public class ContentCheckLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话ID */
    private String sessionId;

    /** 截图ID */
    private Long screenshotId;

    /** 检查结果: PASS/FLAGGED/ERROR */
    private String checkResult;

    /** 风险等级: NONE/LOW/HIGH/CRITICAL */
    private String riskLevel;

    /** 违规分类(JSON数组) */
    private String categories;

    /** 置信度 */
    private Double confidence;

    /** 检查时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkedAt;
}
