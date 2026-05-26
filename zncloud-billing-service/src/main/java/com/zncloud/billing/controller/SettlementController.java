package com.zncloud.billing.controller;

import com.zncloud.billing.model.ApiResult;
import com.zncloud.billing.model.entity.SettlementReport;
import com.zncloud.billing.model.vo.SettlementStatement;
import com.zncloud.billing.service.SettlementRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementRuleService settlementRuleService;

    /**
     * 获取结算报表
     *
     * @param cafeId     网吧ID (可选)
     * @param startDate  开始日期 (yyyy-MM-dd)
     * @param endDate    结束日期 (yyyy-MM-dd)
     * @param reportType 报表类型: DAILY / MONTHLY (可选)
     */
    @GetMapping("/reports")
    public ApiResult<List<SettlementReport>> getReports(
            @RequestParam(required = false) String cafeId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String reportType) {
        List<SettlementReport> reports = settlementRuleService.getReports(cafeId, startDate, endDate, reportType);
        return ApiResult.success(reports);
    }

    /**
     * 获取结算对账单
     *
     * @param cafeId    网吧ID (可选)
     * @param startDate 开始日期 (yyyy-MM-dd)
     * @param endDate   结束日期 (yyyy-MM-dd)
     */
    @GetMapping("/statements")
    public ApiResult<List<SettlementStatement>> getStatements(
            @RequestParam(required = false) String cafeId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        List<SettlementStatement> statements = settlementRuleService.getStatements(cafeId, startDate, endDate);
        return ApiResult.success(statements);
    }
}
