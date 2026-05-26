package com.zncloud.session.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("session")
public class Session {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 用户ID */
    private Long userId;

    /** 设备ID */
    private String deviceId;

    /** 网吧ID */
    private String cafeId;

    /** 设备配置等级 */
    private String deviceConfigLevel;

    /** 每小时价格 */
    private BigDecimal pricePerHour;

    /** 会话状态 */
    private String status;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 持续时长(分钟) */
    private Integer durationMinutes;

    /** 费用(元) */
    private BigDecimal cost;

    /** 断开原因 */
    private String disconnectReason;

    /** 违规详情(JSON) */
    private String violationDetails;

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
