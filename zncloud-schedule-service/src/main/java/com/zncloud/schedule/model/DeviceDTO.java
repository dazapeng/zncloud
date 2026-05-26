package com.zncloud.schedule.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备DTO - 从 device-service 获取的设备数据
 */
@Data
public class DeviceDTO {

    private String id;
    private String cafeId;
    private String cpuInfo;
    private String gpuInfo;
    private Integer memoryGb;
    private Integer diskGb;
    private String osVersion;
    private String macAddress;
    private String publicIp;
    private String status;
    private String configLevel;
    private BigDecimal pricePerHour;
    private LocalDateTime lastOnlineAt;
    private LocalDateTime registeredAt;
    private Double totalOnlineHours;
    private BigDecimal totalEarnings;

    /** 所属网吧信息 (从关联查询补充) */
    private String province;
    private String city;
    private String cafeName;
}
