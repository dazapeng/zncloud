package com.zncloud.session.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContentCheckLogVO {
    private Long id;
    private String sessionId;
    private Long screenshotId;
    private String checkResult;
    private String riskLevel;
    private String categories;
    private Double confidence;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkedAt;

    private String screenshotUrl;
}
