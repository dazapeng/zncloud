package com.zncloud.billing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zncloud.billing.model.entity.SettlementReport;
import com.zncloud.billing.model.vo.SettlementStatement;
import com.zncloud.billing.repository.SettlementReportMapper;
import com.zncloud.billing.service.SettlementRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementRuleServiceImpl implements SettlementRuleService {

    private final SettlementReportMapper settlementReportMapper;

    @Override
    public List<SettlementReport> getReports(String cafeId, String startDate, String endDate, String reportType) {
        LambdaQueryWrapper<SettlementReport> wrapper = new LambdaQueryWrapper<SettlementReport>()
                .eq(StringUtils.hasText(cafeId), SettlementReport::getCafeId, cafeId)
                .eq(StringUtils.hasText(reportType), SettlementReport::getReportType, reportType)
                .ge(StringUtils.hasText(startDate), SettlementReport::getReportDate, startDate)
                .le(StringUtils.hasText(endDate), SettlementReport::getReportDate, endDate)
                .orderByDesc(SettlementReport::getReportDate);

        List<SettlementReport> reports = settlementReportMapper.selectList(wrapper);
        log.info("查询结算报表: cafeId={}, startDate={}, endDate={}, reportType={}, 结果数={}",
                cafeId, startDate, endDate, reportType, reports.size());
        return reports;
    }

    @Override
    public List<SettlementStatement> getStatements(String cafeId, String startDate, String endDate) {
        // 查询月度结算报表作为对账单数据源
        LambdaQueryWrapper<SettlementReport> wrapper = new LambdaQueryWrapper<SettlementReport>()
                .eq(StringUtils.hasText(cafeId), SettlementReport::getCafeId, cafeId)
                .eq(SettlementReport::getReportType, "MONTHLY")
                .ge(StringUtils.hasText(startDate), SettlementReport::getReportDate, startDate)
                .le(StringUtils.hasText(endDate), SettlementReport::getReportDate, endDate)
                .orderByDesc(SettlementReport::getReportDate);

        List<SettlementReport> reports = settlementReportMapper.selectList(wrapper);

        // 转换为对账VO
        List<SettlementStatement> statements = reports.stream().map(report -> {
            SettlementStatement stmt = new SettlementStatement();
            stmt.setId(report.getId());
            stmt.setCafeId(report.getCafeId());
            stmt.setCafeName(report.getCafeName());
            stmt.setPeriod(report.getReportDate());
            stmt.setTotalRevenue(report.getTotalRevenue());
            stmt.setPlatformRevenue(report.getPlatformRevenue());
            stmt.setCafeRevenue(report.getCafeRevenue());
            stmt.setSessionCount(report.getSessionCount());
            stmt.setSettledAt(report.getCreateTime());
            return stmt;
        }).collect(Collectors.toList());

        log.info("查询结算对账单: cafeId={}, startDate={}, endDate={}, 结果数={}",
                cafeId, startDate, endDate, statements.size());
        return statements;
    }
}
