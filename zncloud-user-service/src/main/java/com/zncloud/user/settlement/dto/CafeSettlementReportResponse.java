package com.zncloud.user.settlement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 网吧收益报告
 */
public class CafeSettlementReportResponse {

    // 今日数据
    private BigDecimal todayRevenue;
    private BigDecimal todayCafeShare;
    private Integer todaySessions;
    private BigDecimal todayOnlineHours;

    // 本月数据
    private BigDecimal monthRevenue;
    private BigDecimal monthCafeShare;
    private Integer monthSessions;
    private BigDecimal monthOnlineHours;

    // 累计数据
    private BigDecimal totalRevenue;
    private BigDecimal totalCafeShare;
    private Integer totalSessions;
    private BigDecimal totalOnlineHours;

    // 待结算金额
    private BigDecimal pendingSettlement;

    // 可提现余额
    private BigDecimal withdrawableBalance;

    // 总余额(用户账户)
    private BigDecimal accountBalance;

    // 当前在线设备数
    private Integer onlineDeviceCount;

    // 分成比例
    private BigDecimal commissionRate;

    // ===== Getters & Setters =====

    public BigDecimal getTodayRevenue() { return todayRevenue; }
    public void setTodayRevenue(BigDecimal todayRevenue) { this.todayRevenue = todayRevenue; }

    public BigDecimal getTodayCafeShare() { return todayCafeShare; }
    public void setTodayCafeShare(BigDecimal todayCafeShare) { this.todayCafeShare = todayCafeShare; }

    public Integer getTodaySessions() { return todaySessions; }
    public void setTodaySessions(Integer todaySessions) { this.todaySessions = todaySessions; }

    public BigDecimal getTodayOnlineHours() { return todayOnlineHours; }
    public void setTodayOnlineHours(BigDecimal todayOnlineHours) { this.todayOnlineHours = todayOnlineHours; }

    public BigDecimal getMonthRevenue() { return monthRevenue; }
    public void setMonthRevenue(BigDecimal monthRevenue) { this.monthRevenue = monthRevenue; }

    public BigDecimal getMonthCafeShare() { return monthCafeShare; }
    public void setMonthCafeShare(BigDecimal monthCafeShare) { this.monthCafeShare = monthCafeShare; }

    public Integer getMonthSessions() { return monthSessions; }
    public void setMonthSessions(Integer monthSessions) { this.monthSessions = monthSessions; }

    public BigDecimal getMonthOnlineHours() { return monthOnlineHours; }
    public void setMonthOnlineHours(BigDecimal monthOnlineHours) { this.monthOnlineHours = monthOnlineHours; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public BigDecimal getTotalCafeShare() { return totalCafeShare; }
    public void setTotalCafeShare(BigDecimal totalCafeShare) { this.totalCafeShare = totalCafeShare; }

    public Integer getTotalSessions() { return totalSessions; }
    public void setTotalSessions(Integer totalSessions) { this.totalSessions = totalSessions; }

    public BigDecimal getTotalOnlineHours() { return totalOnlineHours; }
    public void setTotalOnlineHours(BigDecimal totalOnlineHours) { this.totalOnlineHours = totalOnlineHours; }

    public BigDecimal getPendingSettlement() { return pendingSettlement; }
    public void setPendingSettlement(BigDecimal pendingSettlement) { this.pendingSettlement = pendingSettlement; }

    public BigDecimal getWithdrawableBalance() { return withdrawableBalance; }
    public void setWithdrawableBalance(BigDecimal withdrawableBalance) { this.withdrawableBalance = withdrawableBalance; }

    public BigDecimal getAccountBalance() { return accountBalance; }
    public void setAccountBalance(BigDecimal accountBalance) { this.accountBalance = accountBalance; }

    public Integer getOnlineDeviceCount() { return onlineDeviceCount; }
    public void setOnlineDeviceCount(Integer onlineDeviceCount) { this.onlineDeviceCount = onlineDeviceCount; }

    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }
}
