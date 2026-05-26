package com.zncloud.session.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RiskEventVO {
    private Long checkLogId;
    private String sessionId;
    private Long userId;
    private String userPhone;
    private String userName;
    private String deviceId;
    private String deviceMacAddress;
    private String cafeName;
    private String checkResult;
    private String riskLevel;
    private String categories;
    private Double confidence;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkedAt;

    private Long screenshotId;
    private String screenshotThumbnailUrl;

    /** Session info */
    private String sessionStatus;
    private BigDecimal cost;
    private Integer durationMinutes;

    /** 是否已处理 (has audit log entry) */
    private Boolean handled;
}
