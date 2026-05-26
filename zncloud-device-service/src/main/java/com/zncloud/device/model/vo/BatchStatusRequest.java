package com.zncloud.device.model.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchStatusRequest {

    @NotEmpty(message = "设备ID列表不能为空")
    private List<String> deviceIds;

    @NotBlank(message = "状态不能为空")
    private String status;
}
