package com.zncloud.device.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 待处理命令响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingCommandResponse {

    private Long id;
    private String deviceId;
    private String type;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
