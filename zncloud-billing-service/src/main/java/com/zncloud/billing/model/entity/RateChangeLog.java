package com.zncloud.billing.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zncloud.billing.model.enums.ConfigLevel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rate_change_log")
public class RateChangeLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String cafeId;

    private ConfigLevel configLevel;

    private BigDecimal oldPrice;

    private BigDecimal newPrice;

    private String changeType;

    private String operatorId;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
