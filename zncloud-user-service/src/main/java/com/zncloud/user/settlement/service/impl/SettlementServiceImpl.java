package com.zncloud.user.settlement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zncloud.user.mapper.UserMapper;
import com.zncloud.user.model.entity.User;
import com.zncloud.user.settlement.dto.*;
import com.zncloud.user.settlement.entity.CafePartnerInfo;
import com.zncloud.user.settlement.entity.CafeSettlement;
import com.zncloud.user.settlement.entity.Withdrawal;
import com.zncloud.user.settlement.mapper.CafePartnerInfoMapper;
import com.zncloud.user.settlement.mapper.CafeSettlementMapper;
import com.zncloud.user.settlement.mapper.WithdrawalMapper;
import com.zncloud.user.settlement.service.SettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SettlementServiceImpl implements SettlementService {

    @Autowired
    private CafePartnerInfoMapper cafePartnerInfoMapper;

    @Autowired
    private CafeSettlementMapper cafeSettlementMapper;

    @Autowired
    private WithdrawalMapper withdrawalMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public CafeSettlementReportResponse getCafeReport(String cafeId, Long userId) {
        // 1. 校验权限 - 用户必须关联到此网吧
        CafePartnerInfo partner = validateCafeOwnership(cafeId, userId);

        // 2. 查询今日数据
        LocalDate today = LocalDate.now();
        BigDecimal todayRevenue = cafeSettlementMapper.sumTodayRevenue(cafeId, today);
        BigDecimal todayCafeShare = cafeSettlementMapper.sumTodayCafeShare(cafeId, today);
        Integer todaySessions = cafeSettlementMapper.sumTodaySessions(cafeId, today);
        BigDecimal todayOnlineHours = cafeSettlementMapper.sumTodayOnlineHours(cafeId, today);

        // 3. 查询本月数据
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        BigDecimal monthRevenue = cafeSettlementMapper.sumPeriodRevenue(cafeId, monthStart, monthEnd);
        BigDecimal monthCafeShare = cafeSettlementMapper.sumPeriodCafeShare(cafeId, monthStart, monthEnd);
        Integer monthSessions = cafeSettlementMapper.sumPeriodSessions(cafeId, monthStart, monthEnd);
        BigDecimal monthOnlineHours = cafeSettlementMapper.sumPeriodOnlineHours(cafeId, monthStart, monthEnd);

        // 4. 查询累计数据
        BigDecimal totalCafeShare = cafeSettlementMapper.sumTotalCafeShare(cafeId);
        BigDecimal totalRevenue = cafeSettlementMapper.sumTotalRevenue(cafeId);

        // 5. 待结算金额
        BigDecimal pendingSettlement = cafeSettlementMapper.sumPendingSettlement(cafeId);

        // 6. 用户账户余额
        User user = userMapper.selectById(userId);
        BigDecimal accountBalance = user != null ? user.getBalance() : BigDecimal.ZERO;
        BigDecimal withdrawableBalance = accountBalance.compareTo(BigDecimal.ZERO) > 0 ? accountBalance : BigDecimal.ZERO;

        // 7. 构建设响应
        CafeSettlementReportResponse resp = new CafeSettlementReportResponse();
        resp.setTodayRevenue(todayRevenue);
        resp.setTodayCafeShare(todayCafeShare);
        resp.setTodaySessions(todaySessions);
        resp.setTodayOnlineHours(todayOnlineHours);
        resp.setMonthRevenue(monthRevenue);
        resp.setMonthCafeShare(monthCafeShare);
        resp.setMonthSessions(monthSessions);
        resp.setMonthOnlineHours(monthOnlineHours);
        resp.setTotalRevenue(totalRevenue);
        resp.setTotalCafeShare(totalCafeShare);
        resp.setWithdrawableBalance(withdrawableBalance);
        resp.setAccountBalance(accountBalance);
        resp.setPendingSettlement(pendingSettlement);
        resp.setCommissionRate(partner.getCommissionRate());

        // 计算总会话数与总在线时长（从所有已结算记录汇总）
        resp.setTotalSessions(cafeSettlementMapper.sumTotalSessions(cafeId));
        resp.setTotalOnlineHours(cafeSettlementMapper.sumTotalOnlineHours(cafeId));

        return resp;
    }

    @Override
    public List<BillResponse> getCafeBills(String cafeId, Long userId, String periodType) {
        validateCafeOwnership(cafeId, userId);

        List<CafeSettlement> settlements;
        if (periodType != null && !periodType.isEmpty()) {
            settlements = cafeSettlementMapper.findByCafeIdAndPeriodType(cafeId, periodType);
        } else {
            settlements = cafeSettlementMapper.findByCafeId(cafeId);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return settlements.stream().map(s -> {
            BillResponse r = new BillResponse();
            r.setId(s.getId());
            r.setPeriodType(s.getPeriodType());
            r.setPeriodStart(s.getPeriodStart());
            r.setPeriodEnd(s.getPeriodEnd());
            r.setTotalRevenue(s.getTotalRevenue());
            r.setPlatformShare(s.getPlatformShare());
            r.setCafeShare(s.getCafeShare());
            r.setTotalOnlineHours(s.getTotalOnlineHours());
            r.setTotalSessions(s.getTotalSessions());
            r.setDeviceCount(s.getDeviceCount());
            r.setCommissionRate(s.getCommissionRate());
            r.setStatus(s.getStatus());
            r.setCreatedAt(s.getCreatedAt() != null ? s.getCreatedAt().format(fmt) : null);
            return r;
        }).collect(Collectors.toList());
    }

    @Override
    public String exportBillsCsv(String cafeId, Long userId, String periodType) {
        List<BillResponse> bills = getCafeBills(cafeId, userId, periodType);

        StringBuilder sb = new StringBuilder();
        sb.append("ID,周期类型,开始日期,结束日期,总收入,平台分成,网吧分成,在线时长(小时),会话数,设备数,状态\n");
        for (BillResponse b : bills) {
            sb.append(b.getId()).append(",");
            sb.append(b.getPeriodType()).append(",");
            sb.append(b.getPeriodStart()).append(",");
            sb.append(b.getPeriodEnd()).append(",");
            sb.append(b.getTotalRevenue()).append(",");
            sb.append(b.getPlatformShare()).append(",");
            sb.append(b.getCafeShare()).append(",");
            sb.append(b.getTotalOnlineHours()).append(",");
            sb.append(b.getTotalSessions()).append(",");
            sb.append(b.getDeviceCount()).append(",");
            sb.append(b.getStatus()).append("\n");
        }
        return sb.toString();
    }

    @Override
    @Transactional
    public WithdrawalResponse createWithdrawal(CreateWithdrawalRequest request, Long userId) {
        // 1. 校验用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 校验余额
        if (user.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("账户余额不足");
        }

        if (request.getAmount().compareTo(BigDecimal.ONE) < 0) {
            throw new RuntimeException("提现金额不能低于1元");
        }

        // 3. 获取合作商信息
        CafePartnerInfo partner = cafePartnerInfoMapper.findByCafeId(request.getCafeId())
                .orElseThrow(() -> new RuntimeException("未找到网吧合作商信息"));

        // 4. 计算手续费（暂时免除手续费）
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal actualAmount = request.getAmount().subtract(fee);

        // 5. 扣减余额
        BigDecimal beforeBalance = user.getBalance();
        BigDecimal afterBalance = beforeBalance.subtract(request.getAmount());
        user.setBalance(afterBalance);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 6. 创建提现记录
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setUserId(userId);
        withdrawal.setCafeId(request.getCafeId());
        withdrawal.setAmount(request.getAmount());
        withdrawal.setBeforeBalance(beforeBalance);
        withdrawal.setAfterBalance(afterBalance);
        withdrawal.setFee(fee);
        withdrawal.setBankName(request.getBankName() != null ? request.getBankName() : partner.getBankName());
        withdrawal.setBankBranch(request.getBankBranch() != null ? request.getBankBranch() : partner.getBankBranch());
        withdrawal.setBankAccount(request.getBankAccount() != null ? request.getBankAccount() : partner.getBankAccount());
        withdrawal.setAccountHolder(request.getAccountHolder() != null ? request.getAccountHolder() : partner.getAccountHolder());
        withdrawal.setStatus("PENDING");
        withdrawal.setCreatedAt(LocalDateTime.now());
        withdrawal.setUpdatedAt(LocalDateTime.now());
        withdrawalMapper.insert(withdrawal);

        return WithdrawalResponse.fromEntity(withdrawal);
    }

    @Override
    public List<WithdrawalResponse> getWithdrawals(Long userId) {
        List<Withdrawal> withdrawals = withdrawalMapper.findByUserId(userId);
        return withdrawals.stream()
                .map(WithdrawalResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ===== 私有方法 =====

    /**
     * 校验用户是否有权限查看该网吧数据
     */
    private CafePartnerInfo validateCafeOwnership(String cafeId, Long userId) {
        // 先查用户是否有关联到此cafe
        Optional<CafePartnerInfo> byUser = cafePartnerInfoMapper.findByUserId(userId);
        if (byUser.isPresent() && byUser.get().getCafeId().equals(cafeId)) {
            return byUser.get();
        }

        // 再查用户是否直接关联此cafe
        CafePartnerInfo partner = cafePartnerInfoMapper.findByCafeId(cafeId)
                .orElseThrow(() -> new RuntimeException("未找到网吧合作商信息"));

        // 超级管理员可以查看所有
        User user = userMapper.selectById(userId);
        if (user != null && ("SUPER_ADMIN".equals(user.getRole().getCode()) || "OPERATOR".equals(user.getRole().getCode()))) {
            return partner;
        }

        if (!partner.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该网吧的结算数据");
        }

        return partner;
    }
}
