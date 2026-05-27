package com.zncloud.billing.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zncloud.billing.model.entity.BillingRate;
import com.zncloud.billing.model.enums.BillingRateStatus;
import com.zncloud.billing.model.enums.ConfigLevel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BillingRateResponse {
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public static BillingRateResponse from(BillingRate rate) {
        BillingRateResponse r = new BillingRateResponse();
        r.setId(rate.getId());
        r.setCafeId(rate.getCafeId());
        r.setConfigLevel(rate.getConfigLevel());
        r.setPricePerHour(rate.getPricePerHour());
        r.setDiscountStart(rate.getDiscountStart());
        r.setDiscountEnd(rate.getDiscountEnd());
        r.setDiscountRate(rate.getDiscountRate());
        r.setEffectiveAt(rate.getEffectiveAt());
        r.setStatus(rate.getStatus());
        r.setCreatedBy(rate.getCreatedBy());
        r.setRemark(rate.getRemark());
        r.setCreatedAt(rate.getCreatedAt());
        return r;
    }
}
