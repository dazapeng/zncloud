package com.zncloud.device.model.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StatusUpdateRequest {

    @NotBlank(message = "状态不能为空")
    private String status;
}
