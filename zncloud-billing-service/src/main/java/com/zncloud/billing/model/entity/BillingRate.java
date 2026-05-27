package com.zncloud.billing.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zncloud.billing.model.enums.BillingRateStatus;
import com.zncloud.billing.model.enums.ConfigLevel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("billing_rates")
public class BillingRate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String cafeId;

    private ConfigLevel configLevel;

    private BigDecimal pricePerHour;

    private Integer discountStart;

    private Integer discountEnd;

    private BigDecimal discountRate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectiveAt;

    private BillingRateStatus status;

    private String createdBy;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
