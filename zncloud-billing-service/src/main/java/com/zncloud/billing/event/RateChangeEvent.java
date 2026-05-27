package com.zncloud.billing.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zncloud.billing.model.enums.ConfigLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 费率变更事件，通过 Redis Pub/Sub 发布
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateChangeEvent {

    /** 事件ID */
    private String eventId;

    /** 事件类型: RATE_CREATED / RATE_UPDATED / RATE_BATCH_UPDATED */
    private String eventType;

    /** 网吧ID（空字符串表示全局配置） */
    private String cafeId;

    /** 配置等级 */
    private ConfigLevel configLevel;

    /** 旧价格 */
    private BigDecimal oldPrice;

    /** 新价格 */
    private BigDecimal newPrice;

    /** 变更数量（批量时） */
    private Integer affectedCount;

    /** 操作人 */
    private String operatorId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}
