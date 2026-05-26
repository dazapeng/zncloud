package com.zncloud.device.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OperatorDeviceVO {

    /** 设备ID */
    private String id;

    /** 所属网吧ID */
    private String cafeId;

    /** 网吧名称 */
    private String cafeName;

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 区/县 */
    private String district;

    /** 运营商 */
    private String isp;

    /** 配置等级 */
    private String configLevel;

    /** 每小时价格(元) */
    private BigDecimal pricePerHour;

    /** 设备状态 */
    private String status;

    /** 最后在线时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastOnlineAt;

    /** 累计在线时长(小时) */
    private Double totalOnlineHours;

    /** 累计收益(元) */
    private BigDecimal totalEarnings;

    /** CPU信息 */
    private String cpuInfo;

    /** GPU信息 */
    private String gpuInfo;

    /** 内存大小(GB) */
    private Integer memoryGb;
}
