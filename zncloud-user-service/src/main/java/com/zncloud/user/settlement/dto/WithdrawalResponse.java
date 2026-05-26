package com.zncloud.user.settlement.dto;

import com.zncloud.user.settlement.entity.Withdrawal;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现记录响应
 */
public class WithdrawalResponse {

    private Long id;
    private String cafeId;
    private BigDecimal amount;
    private BigDecimal beforeBalance;
    private BigDecimal afterBalance;
    private BigDecimal fee;
    private String bankName;
    private String bankBranch;
    private String bankAccount;
    private String accountHolder;
    private String status;
    private String reviewRemark;
    private LocalDateTime completedTime;
    private LocalDateTime createdAt;

    public static WithdrawalResponse fromEntity(Withdrawal w) {
        WithdrawalResponse resp = new WithdrawalResponse();
        resp.setId(w.getId());
        resp.setCafeId(w.getCafeId());
        resp.setAmount(w.getAmount());
        resp.setBeforeBalance(w.getBeforeBalance());
        resp.setAfterBalance(w.getAfterBalance());
        resp.setFee(w.getFee());
        resp.setBankName(w.getBankName());
        resp.setBankBranch(w.getBankBranch());
        resp.setBankAccount(w.getBankAccount());
        resp.setAccountHolder(w.getAccountHolder());
        resp.setStatus(w.getStatus());
        resp.setReviewRemark(w.getReviewRemark());
        resp.setCompletedTime(w.getCompletedTime());
        resp.setCreatedAt(w.getCreatedAt());
        return resp;
    }

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCafeId() { return cafeId; }
    public void setCafeId(String cafeId) { this.cafeId = cafeId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getBeforeBalance() { return beforeBalance; }
    public void setBeforeBalance(BigDecimal beforeBalance) { this.beforeBalance = beforeBalance; }

    public BigDecimal getAfterBalance() { return afterBalance; }
    public void setAfterBalance(BigDecimal afterBalance) { this.afterBalance = afterBalance; }

    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBankBranch() { return bankBranch; }
    public void setBankBranch(String bankBranch) { this.bankBranch = bankBranch; }

    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }

    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReviewRemark() { return reviewRemark; }
    public void setReviewRemark(String reviewRemark) { this.reviewRemark = reviewRemark; }

    public LocalDateTime getCompletedTime() { return completedTime; }
    public void setCompletedTime(LocalDateTime completedTime) { this.completedTime = completedTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
