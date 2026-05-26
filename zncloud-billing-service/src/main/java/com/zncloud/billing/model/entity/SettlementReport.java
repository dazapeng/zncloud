package com.zncloud.billing.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("settlement_report")
public class SettlementReport {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 所属网吧ID */
    private String cafeId;

    /** 网吧名称 */
    private String cafeName;

    /** 报表日期 (yyyy-MM-dd 或 yyyy-MM) */
    private String reportDate;

    /** 报表类型: DAILY / MONTHLY */
    private String reportType;

    /** 总收入 */
    private BigDecimal totalRevenue;

    /** 平台收入 */
    private BigDecimal platformRevenue;

    /** 网吧收入 */
    private BigDecimal cafeRevenue;

    /** 会话次数 */
    private Integer sessionCount;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
