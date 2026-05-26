package com.zncloud.user.settlement.controller;

import com.zncloud.user.settlement.dto.*;
import com.zncloud.user.settlement.service.SettlementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 网吧结算与提现 API
 */
@RestController
@RequestMapping("/api/v1")
public class SettlementController {

    @Autowired
    private SettlementService settlementService;

    /**
     * 获取网吧收益报告
     */
    @GetMapping("/settlement/cafes/{cafeId}/reports")
    public ResponseEntity<Map<String, Object>> getCafeReport(
            @PathVariable String cafeId,
            @RequestHeader("X-User-Id") Long userId) {
        CafeSettlementReportResponse report = settlementService.getCafeReport(cafeId, userId);
        return ok(report);
    }

    /**
     * 获取账单列表
     */
    @GetMapping("/settlement/cafes/{cafeId}/bills")
    public ResponseEntity<Map<String, Object>> getCafeBills(
            @PathVariable String cafeId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String periodType) {
        List<BillResponse> bills = settlementService.getCafeBills(cafeId, userId, periodType);
        return ok(bills);
    }

    /**
     * 导出账单 CSV
     */
    @GetMapping("/settlement/cafes/{cafeId}/bills/export")
    public ResponseEntity<byte[]> exportBillsCsv(
            @PathVariable String cafeId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String periodType) {
        String csv = settlementService.exportBillsCsv(cafeId, userId, periodType);
        byte[] bytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bills_" + cafeId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
                .body(bytes);
    }

    /**
     * 创建提现申请
     */
    @PostMapping("/withdrawals")
    public ResponseEntity<Map<String, Object>> createWithdrawal(
            @Valid @RequestBody CreateWithdrawalRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        WithdrawalResponse response = settlementService.createWithdrawal(request, userId);
        return ok(response);
    }

    /**
     * 查询提现记录
     */
    @GetMapping("/withdrawals")
    public ResponseEntity<Map<String, Object>> getWithdrawals(
            @RequestHeader("X-User-Id") Long userId) {
        List<WithdrawalResponse> withdrawals = settlementService.getWithdrawals(userId);
        return ok(withdrawals);
    }

    // ===== 统一响应包装 =====

    private ResponseEntity<Map<String, Object>> ok(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);
        return ResponseEntity.ok(result);
    }
}
