package com.zncloud.device.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OperatorStatsVO {

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 区/县 */
    private String district;

    /** 设备总数 */
    private Long deviceCount;

    /** 在线设备数（ONLINE + IN_USE） */
    private Long onlineCount;

    /** 使用中设备数 */
    private Long inUseCount;

    /** 离线设备数 */
    private Long offlineCount;

    /** 在线率 */
    private Double onlineRate;

    /** 利用率（IN_USE / total） */
    private Double utilizationRate;

    /** 平均价格 */
    private BigDecimal avgPrice;

    /** 总收益 */
    private BigDecimal totalEarnings;
}
