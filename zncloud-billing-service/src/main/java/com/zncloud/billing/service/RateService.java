package com.zncloud.billing.service;

import com.zncloud.billing.model.dto.BatchRateRequest;
import com.zncloud.billing.model.dto.BillingRateRequest;
import com.zncloud.billing.model.dto.BillingRateResponse;
import com.zncloud.billing.model.enums.ConfigLevel;

import java.math.BigDecimal;
import java.util.List;

public interface RateService {

    /**
     * 创建费率
     */
    BillingRateResponse createRate(BillingRateRequest request);

    /**
     * 更新费率（将原记录标记为 HISTORY，创建新记录）
     */
    BillingRateResponse updateRate(Long id, BillingRateRequest request);

    /**
     * 查询指定网吧的当前有效费率列表
     */
    List<BillingRateResponse> getRatesByCafe(String cafeId);

    /**
     * 按配置等级查询指定网吧的当前有效费率
     */
    BillingRateResponse getRateByCafeAndLevel(String cafeId, ConfigLevel configLevel);

    /**
     * 获取所有生效中的费率（全局+各网吧覆盖）
     */
    List<BillingRateResponse> getActiveRates();

    /**
     * 批量设置费率（支持100个以上网吧）
     */
    List<BillingRateResponse> batchCreateRates(BatchRateRequest request);

    /**
     * 获取指定网吧+配置等级的历史费率变更记录
     */
    List<BillingRateResponse> getRateHistory(String cafeId, ConfigLevel configLevel);

    /**
     * 计算费用
     */
    BigDecimal calculateCost(String cafeId, ConfigLevel configLevel, int seconds);
}
