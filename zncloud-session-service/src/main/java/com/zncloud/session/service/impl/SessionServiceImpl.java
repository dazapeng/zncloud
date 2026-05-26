package com.zncloud.session.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zncloud.session.model.entity.*;
import com.zncloud.session.model.enums.SessionStatus;
import com.zncloud.session.model.vo.ContentCheckLogVO;
import com.zncloud.session.model.vo.RiskEventVO;
import com.zncloud.session.model.vo.ScreenshotVO;
import com.zncloud.session.model.vo.SessionVO;
import com.zncloud.session.repository.*;
import com.zncloud.session.service.ContentCheckService;
import com.zncloud.session.service.SessionService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionMapper sessionMapper;
    private final SessionScreenshotMapper screenshotMapper;
    private final ContentCheckLogMapper contentCheckLogMapper;
    private final SessionAuditLogMapper auditLogMapper;
    private final MinioClient minioClient;
    private final ObjectMapper objectMapper;

    @Value("${minio.bucket-name:screen-recordings}")
    private String bucketName;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Session createSession(Long userId, String deviceId) {
        Session session = new Session();
        session.setUserId(userId);
        session.setDeviceId(deviceId);
        session.setStatus(SessionStatus.PENDING.name());
        sessionMapper.insert(session);
        log.info("Session created: id={}, userId={}, deviceId={}", session.getId(), userId, deviceId);
        return session;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Session startSession(String sessionId) {
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("Session not found: " + sessionId);
        }
        session.setStatus(SessionStatus.ACTIVE.name());
        session.setStartTime(LocalDateTime.now());
        sessionMapper.updateById(session);
        log.info("Session started: id={}", sessionId);
        return session;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Session endSession(String sessionId) {
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("Session not found: " + sessionId);
        }
        session.setStatus(SessionStatus.COMPLETED.name());
        session.setEndTime(LocalDateTime.now());
        if (session.getStartTime() != null) {
            long minutes = java.time.Duration.between(session.getStartTime(), session.getEndTime()).toMinutes();
            session.setDurationMinutes((int) minutes);
        }
        sessionMapper.updateById(session);
        log.info("Session ended: id={}", sessionId);
        return session;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Session disconnectSession(String sessionId, String reason) {
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("Session not found: " + sessionId);
        }
        session.setStatus(SessionStatus.VIOLATION_DISCONNECTED.name());
        session.setEndTime(LocalDateTime.now());
        session.setDisconnectReason(reason);
        if (session.getStartTime() != null) {
            long minutes = java.time.Duration.between(session.getStartTime(), session.getEndTime()).toMinutes();
            session.setDurationMinutes((int) minutes);
        }
        sessionMapper.updateById(session);

        // Write audit log
        SessionAuditLog auditLog = new SessionAuditLog();
        auditLog.setSessionId(sessionId);
        auditLog.setEventType("SESSION_DISCONNECTED");
        auditLog.setRiskLevel("CRITICAL");
        auditLog.setDescription("Session auto-disconnected due to content violation: " + reason);
        auditLogMapper.insert(auditLog);

        log.warn("Session disconnected due to violation: id={}, reason={}", sessionId, reason);
        return session;
    }

    @Override
    public Session getSessionById(String sessionId) {
        return sessionMapper.selectById(sessionId);
    }

    @Override
    public SessionVO getSessionVO(String sessionId) {
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return null;
        }
        SessionVO vo = new SessionVO();
        vo.setId(session.getId());
        vo.setUserId(session.getUserId());
        vo.setDeviceId(session.getDeviceId());
        vo.setCafeId(session.getCafeId());
        vo.setDeviceConfigLevel(session.getDeviceConfigLevel());
        vo.setPricePerHour(session.getPricePerHour());
        vo.setStatus(session.getStatus());
        vo.setStartTime(session.getStartTime());
        vo.setEndTime(session.getEndTime());
        vo.setDurationMinutes(session.getDurationMinutes());
        vo.setCost(session.getCost());
        vo.setDisconnectReason(session.getDisconnectReason());
        vo.setViolationDetails(session.getViolationDetails());
        vo.setCreateTime(session.getCreateTime());

        // Get recent screenshots
        List<SessionScreenshot> screenshots = screenshotMapper.findLatestBySessionId(sessionId, 10);
        vo.setRecentScreenshots(screenshots.stream().map(this::toScreenshotVO).collect(Collectors.toList()));

        return vo;
    }

    @Override
    public IPage<SessionVO> querySessions(Long userId, String deviceId, String status,
                                          Integer pageNum, Integer pageSize) {
        Page<Session> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20);
        LambdaQueryWrapper<Session> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Session::getUserId, userId);
        }
        if (deviceId != null) {
            wrapper.eq(Session::getDeviceId, deviceId);
        }
        if (status != null) {
            wrapper.eq(Session::getStatus, status);
        }
        wrapper.orderByDesc(Session::getCreateTime);

        IPage<Session> sessionPage = sessionMapper.selectPage(page, wrapper);
        return sessionPage.convert(session -> {
            SessionVO vo = new SessionVO();
            vo.setId(session.getId());
            vo.setUserId(session.getUserId());
            vo.setDeviceId(session.getDeviceId());
            vo.setCafeId(session.getCafeId());
            vo.setDeviceConfigLevel(session.getDeviceConfigLevel());
            vo.setPricePerHour(session.getPricePerHour());
            vo.setStatus(session.getStatus());
            vo.setStartTime(session.getStartTime());
            vo.setEndTime(session.getEndTime());
            vo.setDurationMinutes(session.getDurationMinutes());
            vo.setCost(session.getCost());
            vo.setDisconnectReason(session.getDisconnectReason());
            vo.setViolationDetails(session.getViolationDetails());
            vo.setCreateTime(session.getCreateTime());
            return vo;
        });
    }

    @Override
    public long getActiveSessionCount() {
        LambdaQueryWrapper<Session> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Session::getStatus, SessionStatus.ACTIVE.name());
        return sessionMapper.selectCount(wrapper);
    }

    @Override
    public IPage<RiskEventVO> queryRiskEvents(Integer pageNum, Integer pageSize) {
        Page<ContentCheckLog> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20);
        LambdaQueryWrapper<ContentCheckLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ContentCheckLog::getRiskLevel, "HIGH", "CRITICAL")
                .orderByDesc(ContentCheckLog::getCheckedAt);

        IPage<ContentCheckLog> checkLogPage = contentCheckLogMapper.selectPage(page, wrapper);

        return checkLogPage.convert(this::toRiskEventVO);
    }

    @Override
    public RiskEventVO getRiskEventDetail(Long checkLogId) {
        ContentCheckLog checkLog = contentCheckLogMapper.selectById(checkLogId);
        if (checkLog == null) {
            return null;
        }
        return toRiskEventVO(checkLog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsFalsePositive(Long checkLogId, Long operatorId, String comment) {
        ContentCheckLog checkLog = contentCheckLogMapper.selectById(checkLogId);
        if (checkLog == null) {
            throw new RuntimeException("Content check log not found: " + checkLogId);
        }

        // Update the check log to mark as false positive
        checkLog.setCheckResult("FALSE_POSITIVE");
        checkLog.setRiskLevel("NONE");
        contentCheckLogMapper.updateById(checkLog);

        // Unflag the screenshot
        SessionScreenshot screenshot = screenshotMapper.selectById(checkLog.getScreenshotId());
        if (screenshot != null) {
            screenshot.setFlagged(false);
            screenshotMapper.updateById(screenshot);
        }

        // Write audit log
        SessionAuditLog auditLog = new SessionAuditLog();
        auditLog.setSessionId(checkLog.getSessionId());
        auditLog.setEventType("REVIEW_ACTION");
        auditLog.setRiskLevel("NONE");
        auditLog.setDescription("Marked as false positive");
        auditLog.setOperatorId(operatorId);
        auditLog.setOperatorComment(comment);
        auditLogMapper.insert(auditLog);

        log.info("Content check log {} marked as false positive by operator {}", checkLogId, operatorId);
    }

    // --- Internal helpers ---

    private ScreenshotVO toScreenshotVO(SessionScreenshot entity) {
        ScreenshotVO vo = new ScreenshotVO();
        vo.setId(entity.getId());
        vo.setSessionId(entity.getSessionId());
        vo.setStoragePath(entity.getStoragePath());
        vo.setFileSize(entity.getFileSize());
        vo.setThumbnailPath(entity.getThumbnailPath());
        vo.setFlagged(entity.getFlagged());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setPresignedUrl(generatePresignedUrl(entity.getStoragePath()));
        return vo;
    }

    private RiskEventVO toRiskEventVO(ContentCheckLog checkLog) {
        RiskEventVO vo = new RiskEventVO();
        vo.setCheckLogId(checkLog.getId());
        vo.setSessionId(checkLog.getSessionId());
        vo.setCheckResult(checkLog.getCheckResult());
        vo.setRiskLevel(checkLog.getRiskLevel());
        vo.setCategories(checkLog.getCategories());
        vo.setConfidence(checkLog.getConfidence());
        vo.setCheckedAt(checkLog.getCheckedAt());
        vo.setScreenshotId(checkLog.getScreenshotId());

        // Get screenshot thumbnail
        SessionScreenshot screenshot = screenshotMapper.selectById(checkLog.getScreenshotId());
        if (screenshot != null) {
            vo.setScreenshotThumbnailUrl(generatePresignedUrl(
                    screenshot.getThumbnailPath() != null ? screenshot.getThumbnailPath() : screenshot.getStoragePath()));
        }

        // Check if already handled
        LambdaQueryWrapper<SessionAuditLog> auditWrapper = new LambdaQueryWrapper<>();
        auditWrapper.eq(SessionAuditLog::getSessionId, checkLog.getSessionId())
                .eq(SessionAuditLog::getEventType, "REVIEW_ACTION")
                .last("LIMIT 1");
        vo.setHandled(auditLogMapper.selectCount(auditWrapper) > 0);

        // Get session info
        Session session = sessionMapper.selectById(checkLog.getSessionId());
        if (session != null) {
            vo.setUserId(session.getUserId());
            vo.setDeviceId(session.getDeviceId());
            vo.setSessionStatus(session.getStatus());
            vo.setCost(session.getCost());
            vo.setDurationMinutes(session.getDurationMinutes());
        }

        return vo;
    }

    private String generatePresignedUrl(String objectPath) {
        if (objectPath == null) {
            return null;
        }
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectPath)
                            .expiry(1, TimeUnit.HOURS)
                            .build());
        } catch (Exception e) {
            log.warn("Failed to generate presigned URL for {}: {}", objectPath, e.getMessage());
            return null;
        }
    }
}
