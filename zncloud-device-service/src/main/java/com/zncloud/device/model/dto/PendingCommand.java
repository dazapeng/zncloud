package com.zncloud.device.model.dto;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待处理电源命令实体
 */
@Data
@TableName("pending_command")
public class PendingCommand {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备ID */
    private String deviceId;

    /** 命令类型: REBOOT/POWEROFF */
    private String type;

    /** 状态: PENDING/ACKNOWLEDGED/COMPLETED/FAILED */
    @TableField(fill = FieldFill.INSERT)
    private String status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 确认时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime acknowledgedAt;

    /** 执行结果消息 */
    private String resultMessage;
}
