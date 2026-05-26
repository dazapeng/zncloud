package com.zncloud.schedule.strategy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 分配筛选条件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationCriteria {

    /** 用户ID */
    private String userId;

    /** 区域偏好 (省/市) */
    private String regionPreference;

    /** 配置偏好 */
    private String configPreference;

    /** 最低价格 */
    private BigDecimal priceMin;

    /** 最高价格 */
    private BigDecimal priceMax;
}
