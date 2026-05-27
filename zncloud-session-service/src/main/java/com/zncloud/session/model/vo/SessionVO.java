package com.zncloud.session.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SessionVO {
    private String id;
    private Long userId;
    private String userPhone;
    private String userName;
    private String deviceId;
    private String deviceMacAddress;
    private String cafeId;
    private String cafeName;
    private String deviceConfigLevel;
    private BigDecimal pricePerHour;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Integer durationMinutes;
    private BigDecimal cost;
    private String disconnectReason;
    private String violationDetails;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private List<ScreenshotVO> recentScreenshots;
}
