package com.zncloud.device.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConfigStatsVO {

    /** 配置等级 */
    private String configLevel;

    /** 设备总数 */
    private Long deviceCount;

    /** 在线设备数 */
    private Long onlineCount;

    /** 利用率（IN_USE / total） */
    private Double utilizationRate;

    /** 平均价格 */
    private BigDecimal avgPrice;
}
