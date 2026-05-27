package com.zncloud.billing.model.dto;

import com.zncloud.billing.model.enums.ConfigLevel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BatchRateRequest {
    private List<String> cafeIds;
    private ConfigLevel configLevel;
    private BigDecimal pricePerHour;
    private Integer discountStart;
    private Integer discountEnd;
    private BigDecimal discountRate;
    private LocalDateTime effectiveAt;
    private String operatorId;
    private String remark;
}
