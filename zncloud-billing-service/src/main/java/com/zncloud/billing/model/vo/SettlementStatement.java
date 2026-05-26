package com.zncloud.billing.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SettlementStatement {

    /** 结算ID */
    private String id;

    /** 网吧ID */
    private String cafeId;

    /** 网吧名称 */
    private String cafeName;

    /** 结算周期 (年月, e.g. 2025-01) */
    private String period;

    /** 总收入 */
    private BigDecimal totalRevenue;

    /** 平台收入 */
    private BigDecimal platformRevenue;

    /** 网吧收入 */
    private BigDecimal cafeRevenue;

    /** 会话次数 */
    private Integer sessionCount;

    /** 结算时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime settledAt;
}
