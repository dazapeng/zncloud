package com.zncloud.billing.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("settlement_rule")
public class SettlementRule {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 所属网吧ID */
    private String cafeId;

    /** 平台分成比例 */
    private BigDecimal platformRate;

    /** 网吧分成比例 */
    private BigDecimal cafeRate;

    /** 规则状态 */
    private SettlementRuleStatus status;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
