package com.zncloud.billing.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zncloud.billing.model.ApiResult;
import com.zncloud.billing.model.entity.AuditLog;
import com.zncloud.billing.repository.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogMapper auditLogMapper;

    /**
     * 分页查询审计日志，支持复合条件筛选
     *
     * @param userId    用户ID (可选)
     * @param action    操作类型 (可选)
     * @param startTime 开始时间 (可选)
     * @param endTime   结束时间 (可选)
     * @param pageNum   页码 (默认1)
     * @param pageSize  每页条数 (默认20)
     */
    @GetMapping
    public ApiResult<IPage<AuditLog>> listAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {

        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<AuditLog>()
                .eq(userId != null, AuditLog::getUserId, userId)
                .eq(StringUtils.hasText(action), AuditLog::getAction, action)
                .ge(startTime != null, AuditLog::getCreatedAt, startTime)
                .le(endTime != null, AuditLog::getCreatedAt, endTime)
                .orderByDesc(AuditLog::getCreatedAt);

        Page<AuditLog> page = new Page<>(pageNum, pageSize);
        IPage<AuditLog> result = auditLogMapper.selectPage(page, wrapper);

        log.info("查询审计日志: userId={}, action={}, startTime={}, endTime={}, pageNum={}, pageSize={}, 总数={}",
                userId, action, startTime, endTime, pageNum, pageSize, result.getTotal());
        return ApiResult.success(result);
    }
}
