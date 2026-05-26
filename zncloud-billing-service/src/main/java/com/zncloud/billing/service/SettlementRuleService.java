package com.zncloud.billing.service;

import com.zncloud.billing.model.entity.SettlementReport;
import com.zncloud.billing.model.vo.SettlementStatement;

import java.util.List;

public interface SettlementRuleService {

    /**
     * 获取结算报表
     *
     * @param cafeId     网吧ID (可选)
     * @param startDate  开始日期 (yyyy-MM-dd)
     * @param endDate    结束日期 (yyyy-MM-dd)
     * @param reportType 报表类型: DAILY / MONTHLY (可选)
     * @return 结算报表列表
     */
    List<SettlementReport> getReports(String cafeId, String startDate, String endDate, String reportType);

    /**
     * 获取结算对账单
     *
     * @param cafeId    网吧ID (可选)
     * @param startDate 开始日期 (yyyy-MM-dd)
     * @param endDate   结束日期 (yyyy-MM-dd)
     * @return 结算对账单列表
     */
    List<SettlementStatement> getStatements(String cafeId, String startDate, String endDate);
}
