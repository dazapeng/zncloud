package com.zncloud.session.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScreenshotVO {
    private Long id;
    private String sessionId;
    private String storagePath;
    private Integer fileSize;
    private String thumbnailPath;
    private Boolean flagged;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private String presignedUrl;
}
