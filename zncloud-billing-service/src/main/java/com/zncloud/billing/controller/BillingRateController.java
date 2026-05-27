package com.zncloud.billing.controller;

import com.zncloud.billing.model.ApiResult;
import com.zncloud.billing.model.dto.BatchRateRequest;
import com.zncloud.billing.model.dto.BillingRateRequest;
import com.zncloud.billing.model.dto.BillingRateResponse;
import com.zncloud.billing.model.enums.ConfigLevel;
import com.zncloud.billing.service.RateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing/rates")
@RequiredArgsConstructor
public class BillingRateController {

    private final RateService rateService;

    /**
     * 查询指定网吧的费率列表
     * GET /api/v1/billing/rates?cafeId={cafeId}
     */
    @GetMapping
    public ApiResult<List<BillingRateResponse>> getRates(@RequestParam(required = false) String cafeId) {
        List<BillingRateResponse> rates = rateService.getRatesByCafe(cafeId);
        return ApiResult.success(rates);
    }

    /**
     * 创建/更新费率
     * POST /api/v1/billing/rates
     */
    @PostMapping
    public ApiResult<BillingRateResponse> createRate(@RequestBody BillingRateRequest request) {
        try {
            BillingRateResponse rate = rateService.createRate(request);
            return ApiResult.success(rate);
        } catch (IllegalArgumentException e) {
            return ApiResult.badRequest(e.getMessage());
        }
    }

    /**
     * 更新指定费率为新价格（将原记录标记为历史，创建新记录）
     * PUT /api/v1/billing/rates/{id}
     */
    @PutMapping("/{id}")
    public ApiResult<BillingRateResponse> updateRate(@PathVariable Long id,
                                                     @RequestBody BillingRateRequest request) {
        try {
            BillingRateResponse rate = rateService.updateRate(id, request);
            return ApiResult.success(rate);
        } catch (IllegalArgumentException e) {
            return ApiResult.badRequest(e.getMessage());
        }
    }

    /**
     * 批量设置费率（一次请求最多500个网吧）
     * POST /api/v1/billing/rates/batch
     */
    @PostMapping("/batch")
    public ApiResult<List<BillingRateResponse>> batchCreateRates(@RequestBody BatchRateRequest request) {
        try {
            List<BillingRateResponse> rates = rateService.batchCreateRates(request);
            return ApiResult.success(rates);
        } catch (IllegalArgumentException e) {
            return ApiResult.badRequest(e.getMessage());
        }
    }

    /**
     * 获取历史费率变更记录
     * GET /api/v1/billing/rates/history?cafeId={cafeId}&configLevel={level}
     */
    @GetMapping("/history")
    public ApiResult<List<BillingRateResponse>> getRateHistory(
            @RequestParam(required = false) String cafeId,
            @RequestParam(required = false) ConfigLevel configLevel) {
        List<BillingRateResponse> history = rateService.getRateHistory(cafeId, configLevel);
        return ApiResult.success(history);
    }

    /**
     * 获取所有生效中的费率
     * GET /api/v1/billing/rates/active
     */
    @GetMapping("/active")
    public ApiResult<List<BillingRateResponse>> getActiveRates() {
        List<BillingRateResponse> rates = rateService.getActiveRates();
        return ApiResult.success(rates);
    }

    /**
     * 按配置等级查询指定网吧的费率
     * GET /api/v1/billing/rates/{configLevel}?cafeId={cafeId}
     */
    @GetMapping("/{configLevel}")
    public ApiResult<BillingRateResponse> getRateByLevel(
            @PathVariable ConfigLevel configLevel,
            @RequestParam(required = false) String cafeId) {
        try {
            BillingRateResponse rate = rateService.getRateByCafeAndLevel(cafeId, configLevel);
            return ApiResult.success(rate);
        } catch (IllegalArgumentException e) {
            return ApiResult.notFound(e.getMessage());
        }
    }

    /**
     * 计算费率
     * GET /api/v1/billing/rates/{configLevel}/calculate?cafeId={cafeId}&seconds={seconds}
     */
    @GetMapping("/{configLevel}/calculate")
    public ApiResult<Map<String, Object>> calculateCost(
            @PathVariable ConfigLevel configLevel,
            @RequestParam(required = false) String cafeId,
            @RequestParam int seconds) {
        BigDecimal cost = rateService.calculateCost(cafeId, configLevel, seconds);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cafeId", cafeId != null ? cafeId : "全局");
        result.put("configLevel", configLevel);
        result.put("seconds", seconds);
        result.put("cost", cost);
        return ApiResult.success(result);
    }
}
