package com.zncloud.device.model.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NotificationRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    /** 通知类型：SYSTEM（系统通知）/ MAINTENANCE（维护通知）/ PROMOTION（活动通知） */
    @NotBlank(message = "通知类型不能为空")
    private String type;

    /** 目标类型：ALL（全部）/ PROVINCE（按省份）/ CAFE（按网吧）/ DEVICE（按设备） */
    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    /** 目标值：当 targetType 为 PROVINCE/CAFE/DEVICE 时对应具体的值 */
    private String targetValue;
}
