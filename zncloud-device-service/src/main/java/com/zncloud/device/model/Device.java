package com.zncloud.device.model;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("device")
public class Device {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 所属网吧ID */
    private String cafeId;

    /** CPU信息 */
    private String cpuInfo;

    /** GPU信息 */
    private String gpuInfo;

    /** 内存大小(GB) */
    private Integer memoryGb;

    /** 磁盘大小(GB) */
    private Integer diskGb;

    /** 操作系统版本 */
    private String osVersion;

    /** 省份（从所属网吧填充） */
    private String province;

    /** 城市（从所属网吧填充） */
    private String city;

    /** 网吧名称（从所属网吧填充） */
    private String cafeName;

    /** MAC地址(唯一标识) */
    private String macAddress;

    /** 公网IP */
    private String publicIp;

    /** 设备状态 */
    private DeviceStatus status;

    /** 配置等级 */
    private ConfigLevel configLevel;

    /** 每小时价格(元) */
    private BigDecimal pricePerHour;

    /** 最后在线时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastOnlineAt;

    /** 注册时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime registeredAt;

    /** 累计在线时长(小时) */
    private Double totalOnlineHours;

    /** 累计收益(元) */
    private BigDecimal totalEarnings;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("is_deleted")
    private Integer deleted;
}
