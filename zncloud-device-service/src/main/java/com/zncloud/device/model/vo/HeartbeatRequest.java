package com.zncloud.device.model.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HeartbeatRequest {

    @NotBlank(message = "设备ID不能为空")
    private String deviceId;
}
