package com.zncloud.user.settlement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 账单记录响应
 */
public class BillResponse {

    private Long id;
    private String periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalRevenue;
    private BigDecimal platformShare;
    private BigDecimal cafeShare;
    private BigDecimal totalOnlineHours;
    private Integer totalSessions;
    private Integer deviceCount;
    private BigDecimal commissionRate;
    private String status;
    private String createdAt;

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPeriodType() { return periodType; }
    public void setPeriodType(String periodType) { this.periodType = periodType; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public BigDecimal getPlatformShare() { return platformShare; }
    public void setPlatformShare(BigDecimal platformShare) { this.platformShare = platformShare; }

    public BigDecimal getCafeShare() { return cafeShare; }
    public void setCafeShare(BigDecimal cafeShare) { this.cafeShare = cafeShare; }

    public BigDecimal getTotalOnlineHours() { return totalOnlineHours; }
    public void setTotalOnlineHours(BigDecimal totalOnlineHours) { this.totalOnlineHours = totalOnlineHours; }

    public Integer getTotalSessions() { return totalSessions; }
    public void setTotalSessions(Integer totalSessions) { this.totalSessions = totalSessions; }

    public Integer getDeviceCount() { return deviceCount; }
    public void setDeviceCount(Integer deviceCount) { this.deviceCount = deviceCount; }

    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
