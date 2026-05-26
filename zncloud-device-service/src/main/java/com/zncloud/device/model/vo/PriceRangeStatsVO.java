package com.zncloud.device.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PriceRangeStatsVO {

    /** 价格区间名称，如 "0-1元", "1-2元", "2-3元", "3+元" */
    private String rangeName;

    /** 设备总数 */
    private Long deviceCount;

    /** 在线设备数 */
    private Long onlineCount;

    /** 平均价格 */
    private BigDecimal avgPrice;
}
