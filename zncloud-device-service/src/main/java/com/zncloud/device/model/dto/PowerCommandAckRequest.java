package com.zncloud.device.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 电源命令确认请求 DTO
 */
@Data
public class PowerCommandAckRequest {

    @NotNull(message = "命令ID不能为空")
    private Long commandId;

    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    /** 执行结果: COMPLETED/FAILED */
    @NotBlank(message = "执行结果不能为空")
    private String result;

    /** 结果消息 */
    private String message;
}
