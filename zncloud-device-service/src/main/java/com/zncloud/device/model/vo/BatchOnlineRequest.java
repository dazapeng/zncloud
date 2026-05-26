package com.zncloud.device.model.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BatchOnlineRequest {

    @NotEmpty(message = "设备ID列表不能为空")
    private List<String> deviceIds;

    @NotNull(message = "操作类型不能为空")
    private String action; // ONLINE 或 OFFLINE
}
