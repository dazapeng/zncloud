package com.zncloud.device.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class IspStatsVO {

    /** 运营商 */
    private String isp;

    /** 设备总数 */
    private Long deviceCount;

    /** 在线设备数 */
    private Long onlineCount;

    /** 平均价格 */
    private BigDecimal avgPrice;
}
