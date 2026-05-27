package com.zncloud.billing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zncloud.billing.event.RateChangeEvent;
import com.zncloud.billing.event.RateChangeEventPublisher;
import com.zncloud.billing.model.dto.BatchRateRequest;
import com.zncloud.billing.model.dto.BillingRateRequest;
import com.zncloud.billing.model.dto.BillingRateResponse;
import com.zncloud.billing.model.entity.BillingRate;
import com.zncloud.billing.model.entity.RateChangeLog;
import com.zncloud.billing.model.enums.BillingRateStatus;
import com.zncloud.billing.model.enums.ConfigLevel;
import com.zncloud.billing.repository.BillingRateMapper;
import com.zncloud.billing.repository.RateChangeLogMapper;
import com.zncloud.billing.service.RateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateServiceImpl implements RateService {

    private final BillingRateMapper billingRateMapper;
    private final RateChangeLogMapper rateChangeLogMapper;
    private final RateChangeEventPublisher eventPublisher;

    // ==================== 创建费率 ====================

    @Override
    @Transactional
    public BillingRateResponse createRate(BillingRateRequest request) {
        String cafeId = request.getCafeId() != null ? request.getCafeId() : "";
        validateRateRequest(request);

        // 查找当前有效记录，将其标记为 HISTORY
        BillingRate current = findActiveRate(cafeId, request.getConfigLevel());
        BigDecimal oldPrice = current != null ? current.getPricePerHour() : null;

        if (current != null) {
            current.setStatus(BillingRateStatus.HISTORY);
            billingRateMapper.updateById(current);
        }

        // 创建新费率记录
        BillingRate rate = buildNewRate(request, cafeId);
        billingRateMapper.insert(rate);

        // 记录变更日志
        logRateChange(cafeId, request.getConfigLevel(), oldPrice, request.getPricePerHour(),
                "SINGLE", request.getCreatedBy(), request.getRemark());

        // 发布事件通知
        eventPublisher.publish(RateChangeEvent.builder()
                .eventType("RATE_CREATED")
                .cafeId(cafeId)
                .configLevel(request.getConfigLevel())
                .oldPrice(oldPrice)
                .newPrice(request.getPricePerHour())
                .affectedCount(1)
                .operatorId(request.getCreatedBy())
                .build());

        log.info("费率创建成功: cafeId={}, configLevel={}, price={}/h", cafeId, request.getConfigLevel(), request.getPricePerHour());
        return BillingRateResponse.from(rate);
    }

    // ==================== 更新费率 ====================

    @Override
    @Transactional
    public BillingRateResponse updateRate(Long id, BillingRateRequest request) {
        BillingRate rate = billingRateMapper.selectById(id);
        if (rate == null) {
            throw new IllegalArgumentException("费率不存在: " + id);
        }

        BigDecimal oldPrice = rate.getPricePerHour();

        // 将原记录标记为 HISTORY
        rate.setStatus(BillingRateStatus.HISTORY);
        billingRateMapper.updateById(rate);

        // 创建新费率记录
        BillingRate newRate = mergeRateUpdate(rate, request);
        billingRateMapper.insert(newRate);

        // 记录变更日志
        logRateChange(newRate.getCafeId(), newRate.getConfigLevel(), oldPrice, newRate.getPricePerHour(),
                "SINGLE", request.getCreatedBy(), request.getRemark());

        // 发布事件通知
        eventPublisher.publish(RateChangeEvent.builder()
                .eventType("RATE_UPDATED")
                .cafeId(newRate.getCafeId())
                .configLevel(newRate.getConfigLevel())
                .oldPrice(oldPrice)
                .newPrice(newRate.getPricePerHour())
                .affectedCount(1)
                .operatorId(request.getCreatedBy())
                .build());

        log.info("费率更新成功: id={} -> newId={}, cafeId={}, configLevel={}, oldPrice={}, newPrice={}",
                id, newRate.getId(), newRate.getCafeId(), newRate.getConfigLevel(), oldPrice, newRate.getPricePerHour());

        return BillingRateResponse.from(newRate);
    }

    // ==================== 查询费率 ====================

    @Override
    public List<BillingRateResponse> getRatesByCafe(String cafeId) {
        String queryCafeId = cafeId != null ? cafeId : "";
        List<BillingRate> rates = billingRateMapper.selectList(
                new LambdaQueryWrapper<BillingRate>()
                        .eq(BillingRate::getCafeId, queryCafeId)
                        .eq(BillingRate::getStatus, BillingRateStatus.ACTIVE)
                        .orderByDesc(BillingRate::getEffectiveAt));
        return rates.stream().map(BillingRateResponse::from).collect(Collectors.toList());
    }

    @Override
    public BillingRateResponse getRateByCafeAndLevel(String cafeId, ConfigLevel configLevel) {
        String queryCafeId = cafeId != null ? cafeId : "";

        // 先查询网吧级配置
        BillingRate rate = findActiveRate(queryCafeId, configLevel);

        // 如果没找到网吧级配置，回退到全局配置
        if (rate == null && !queryCafeId.isEmpty()) {
            rate = findActiveRate("", configLevel);
        }

        if (rate == null) {
            throw new IllegalArgumentException("未找到有效的费率配置: configLevel=" + configLevel
                    + ", cafeId=" + (cafeId != null ? cafeId : "全局"));
        }

        return BillingRateResponse.from(rate);
    }

    @Override
    public List<BillingRateResponse> getActiveRates() {
        List<BillingRate> rates = billingRateMapper.selectList(
                new LambdaQueryWrapper<BillingRate>()
                        .eq(BillingRate::getStatus, BillingRateStatus.ACTIVE)
                        .le(BillingRate::getEffectiveAt, LocalDateTime.now())
                        .orderByDesc(BillingRate::getEffectiveAt));

        // 按 (cafeId, configLevel) 去重，保留最新的
        Map<String, BillingRate> latestRates = new LinkedHashMap<>();
        for (BillingRate rate : rates) {
            String key = rate.getCafeId() + ":" + rate.getConfigLevel();
            latestRates.merge(key, rate, (existing, replacement) ->
                    existing.getEffectiveAt().isAfter(replacement.getEffectiveAt()) ? existing : replacement);
        }

        return latestRates.values().stream()
                .sorted(Comparator.comparing(BillingRate::getCafeId)
                        .thenComparing(BillingRate::getConfigLevel))
                .map(BillingRateResponse::from)
                .collect(Collectors.toList());
    }

    // ==================== 批量设置费率 ====================

    @Override
    @Transactional
    public List<BillingRateResponse> batchCreateRates(BatchRateRequest request) {
        if (request.getCafeIds() == null || request.getCafeIds().isEmpty()) {
            throw new IllegalArgumentException("网吧ID列表不能为空");
        }
        if (request.getConfigLevel() == null) {
            throw new IllegalArgumentException("配置等级不能为空");
        }
        if (request.getPricePerHour() == null || request.getPricePerHour().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("价格必须大于0");
        }

        if (request.getCafeIds().size() > 500) {
            throw new IllegalArgumentException("单次批量操作最多支持500个网吧");
        }

        List<BillingRateResponse> results = new ArrayList<>();
        LocalDateTime effectiveAt = request.getEffectiveAt() != null ? request.getEffectiveAt() : LocalDateTime.now();

        for (String cafeId : request.getCafeIds()) {
            String safeCafeId = cafeId != null ? cafeId : "";

            // 将旧的 ACTIVE 记录标记为 HISTORY
            BillingRate current = findActiveRate(safeCafeId, request.getConfigLevel());
            if (current != null) {
                current.setStatus(BillingRateStatus.HISTORY);
                billingRateMapper.updateById(current);
            }

            // 创建新费率
            BillingRate rate = new BillingRate();
            rate.setCafeId(safeCafeId);
            rate.setConfigLevel(request.getConfigLevel());
            rate.setPricePerHour(request.getPricePerHour());
            rate.setDiscountStart(request.getDiscountStart());
            rate.setDiscountEnd(request.getDiscountEnd());
            rate.setDiscountRate(request.getDiscountRate());
            rate.setEffectiveAt(effectiveAt);
            rate.setStatus(BillingRateStatus.ACTIVE);
            rate.setCreatedBy(request.getOperatorId());
            rate.setRemark(request.getRemark());

            billingRateMapper.insert(rate);
            results.add(BillingRateResponse.from(rate));

            // 记录变更日志
            logRateChange(safeCafeId, request.getConfigLevel(), current != null ? current.getPricePerHour() : null,
                    request.getPricePerHour(), "BATCH", request.getOperatorId(),
                    "批量调价: " + (request.getRemark() != null ? request.getRemark() : ""));
        }

        // 发布批量变更事件
        eventPublisher.publish(RateChangeEvent.builder()
                .eventType("RATE_BATCH_UPDATED")
                .configLevel(request.getConfigLevel())
                .newPrice(request.getPricePerHour())
                .affectedCount(results.size())
                .operatorId(request.getOperatorId())
                .build());

        log.info("批量费率创建完成: configLevel={}, price={}/h, count={}",
                request.getConfigLevel(), request.getPricePerHour(), results.size());

        return results;
    }

    // ==================== 历史记录 ====================

    @Override
    public List<BillingRateResponse> getRateHistory(String cafeId, ConfigLevel configLevel) {
        LambdaQueryWrapper<BillingRate> wrapper = new LambdaQueryWrapper<BillingRate>()
                .orderByDesc(BillingRate::getEffectiveAt);

        if (cafeId != null && !cafeId.isEmpty()) {
            wrapper.eq(BillingRate::getCafeId, cafeId);
        }
        if (configLevel != null) {
            wrapper.eq(BillingRate::getConfigLevel, configLevel);
        }

        // 查询所有状态（ACTIVE + HISTORY）的记录，按生效时间倒序
        List<BillingRate> rates = billingRateMapper.selectList(wrapper);
        return rates.stream().map(BillingRateResponse::from).collect(Collectors.toList());
    }

    // ==================== 费用计算 ====================

    @Override
    public BigDecimal calculateCost(String cafeId, ConfigLevel configLevel, int seconds) {
        BillingRateResponse rate = getRateByCafeAndLevel(cafeId, configLevel);
        BigDecimal perSecond = rate.getPricePerHour()
                .divide(BigDecimal.valueOf(3600), 10, RoundingMode.HALF_UP);
        BigDecimal cost = perSecond.multiply(BigDecimal.valueOf(seconds));
        return cost.setScale(2, RoundingMode.HALF_UP);
    }

    // ==================== 私有方法 ====================

    private void validateRateRequest(BillingRateRequest request) {
        if (request.getConfigLevel() == null) {
            throw new IllegalArgumentException("配置等级不能为空");
        }
        if (request.getPricePerHour() == null || request.getPricePerHour().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("价格必须大于0");
        }
    }

    private BillingRate findActiveRate(String cafeId, ConfigLevel configLevel) {
        return billingRateMapper.selectOne(
                new LambdaQueryWrapper<BillingRate>()
                        .eq(BillingRate::getCafeId, cafeId)
                        .eq(BillingRate::getConfigLevel, configLevel)
                        .eq(BillingRate::getStatus, BillingRateStatus.ACTIVE)
                        .le(BillingRate::getEffectiveAt, LocalDateTime.now())
                        .orderByDesc(BillingRate::getEffectiveAt)
                        .last("LIMIT 1"));
    }

    private BillingRate buildNewRate(BillingRateRequest request, String cafeId) {
        BillingRate rate = new BillingRate();
        rate.setCafeId(cafeId);
        rate.setConfigLevel(request.getConfigLevel());
        rate.setPricePerHour(request.getPricePerHour());
        rate.setDiscountStart(request.getDiscountStart());
        rate.setDiscountEnd(request.getDiscountEnd());
        rate.setDiscountRate(request.getDiscountRate());
        rate.setEffectiveAt(request.getEffectiveAt() != null ? request.getEffectiveAt() : LocalDateTime.now());
        rate.setStatus(BillingRateStatus.ACTIVE);
        rate.setCreatedBy(request.getCreatedBy());
        rate.setRemark(request.getRemark());
        return rate;
    }

    private BillingRate mergeRateUpdate(BillingRate existing, BillingRateRequest request) {
        BillingRate rate = new BillingRate();
        rate.setCafeId(request.getCafeId() != null ? request.getCafeId() : existing.getCafeId());
        rate.setConfigLevel(request.getConfigLevel() != null ? request.getConfigLevel() : existing.getConfigLevel());
        rate.setPricePerHour(request.getPricePerHour() != null ? request.getPricePerHour() : existing.getPricePerHour());
        rate.setDiscountStart(request.getDiscountStart() != null ? request.getDiscountStart() : existing.getDiscountStart());
        rate.setDiscountEnd(request.getDiscountEnd() != null ? request.getDiscountEnd() : existing.getDiscountEnd());
        rate.setDiscountRate(request.getDiscountRate() != null ? request.getDiscountRate() : existing.getDiscountRate());
        rate.setEffectiveAt(request.getEffectiveAt() != null ? request.getEffectiveAt() : LocalDateTime.now());
        rate.setStatus(BillingRateStatus.ACTIVE);
        rate.setCreatedBy(request.getCreatedBy());
        rate.setRemark(request.getRemark());
        return rate;
    }

    private void logRateChange(String cafeId, ConfigLevel configLevel, BigDecimal oldPrice,
                               BigDecimal newPrice, String changeType, String operatorId, String remark) {
        RateChangeLog logEntry = new RateChangeLog();
        logEntry.setCafeId(cafeId);
        logEntry.setConfigLevel(configLevel);
        logEntry.setOldPrice(oldPrice);
        logEntry.setNewPrice(newPrice);
        logEntry.setChangeType(changeType);
        logEntry.setOperatorId(operatorId);
        logEntry.setRemark(remark);
        rateChangeLogMapper.insert(logEntry);
    }
}
