package com.zncloud.billing.model.dto;

import com.zncloud.billing.model.enums.ConfigLevel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BillingRateRequest {
    private String cafeId;
    private ConfigLevel configLevel;
    private BigDecimal pricePerHour;
    private Integer discountStart;
    private Integer discountEnd;
    private BigDecimal discountRate;
    private LocalDateTime effectiveAt;
    private String createdBy;
    private String remark;
}
