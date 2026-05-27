package com.zncloud.user.settlement.service;

import com.zncloud.user.settlement.dto.*;
import java.util.List;

public interface SettlementService {

    /**
     * 获取网吧收益报告
     */
    CafeSettlementReportResponse getCafeReport(String cafeId, Long userId);

    /**
     * 获取账单列表
     */
    List<BillResponse> getCafeBills(String cafeId, Long userId, String periodType);

    /**
     * 导出账单CSV
     */
    String exportBillsCsv(String cafeId, Long userId, String periodType);

    /**
     * 创建提现申请
     */
    WithdrawalResponse createWithdrawal(CreateWithdrawalRequest request, Long userId);

    /**
     * 查询提现记录
     */
    List<WithdrawalResponse> getWithdrawals(Long userId);
}
