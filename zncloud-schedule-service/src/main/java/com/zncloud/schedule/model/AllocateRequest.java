package com.zncloud.schedule.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 调度请求参数
 */
@Data
public class AllocateRequest {

    /** 用户ID */
    private String userId;

    /** 区域偏好 (省/市) */
    private String regionPreference;

    /** 配置偏好 ENTRY / MAINSTREAM / HIGH_PERFORMANCE */
    private String configPreference;

    /** 最低价格 */
    private BigDecimal priceMin;

    /** 最高价格 */
    private BigDecimal priceMax;
}
