package com.zncloud.schedule.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 调度分配响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocateResponse {

    /** 设备ID */
    private String deviceId;

    /** 网吧ID */
    private String cafeId;

    /** 设备IP */
    private String deviceIp;

    /** 连接令牌 */
    private String connectionToken;

    /** 区域信息 (省-市) */
    private String region;

    /** 配置等级 */
    private String configLevel;
}
