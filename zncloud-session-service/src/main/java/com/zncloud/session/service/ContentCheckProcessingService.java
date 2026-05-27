package com.zncloud.session.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zncloud.session.model.entity.ContentCheckLog;
import com.zncloud.session.model.entity.SessionScreenshot;
import com.zncloud.session.repository.ContentCheckLogMapper;
import com.zncloud.session.repository.SessionScreenshotMapper;
import com.zncloud.session.service.ContentCheckService.CheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 内容检查处理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentCheckProcessingService {

    private final ContentCheckService contentCheckService;
    private final ContentCheckLogMapper contentCheckLogMapper;
    private final SessionScreenshotMapper screenshotMapper;
    private final SessionService sessionService;

    /**
     * 对截图执行内容检查
     *
     * @param screenshotId 截图ID
     * @return 检查结果
     */
    @Transactional(rollbackFor = Exception.class)
    public CheckResult checkScreenshot(Long screenshotId) {
        SessionScreenshot screenshot = screenshotMapper.selectById(screenshotId);
        if (screenshot == null) {
            throw new RuntimeException("Screenshot not found: " + screenshotId);
        }

        // Perform content check
        CheckResult result = contentCheckService.checkImage(screenshot.getStoragePath());

        // Save check log
        ContentCheckLog checkLog = new ContentCheckLog();
        checkLog.setSessionId(screenshot.getSessionId());
        checkLog.setScreenshotId(screenshotId);
        checkLog.setCheckResult(result.getResult());
        checkLog.setRiskLevel(result.getRiskLevel().name());
        checkLog.setCategories(result.getCategories() != null
                ? String.join(",", result.getCategories()) : null);
        checkLog.setConfidence(result.getConfidence());
        checkLog.setCheckedAt(LocalDateTime.now());
        contentCheckLogMapper.insert(checkLog);

        // If flagged, mark screenshot
        if (!result.isPass()) {
            screenshot.setFlagged(true);
            screenshotMapper.updateById(screenshot);
            log.warn("Content violation detected - screenshotId: {}, risk: {}, categories: {}",
                    screenshotId, result.getRiskLevel(), result.getCategories());
        }

        // Handle CRITICAL violations - auto disconnect session
        if (result.getRiskLevel() == CheckResult.RiskLevel.CRITICAL) {
            String violationJson = String.format(
                    "{\"riskLevel\":\"%s\",\"categories\":[\"%s\"],\"confidence\":%f,\"screenshotId\":%d}",
                    result.getRiskLevel(),
                    result.getCategories() != null ? String.join("\",\"", result.getCategories()) : "",
                    result.getConfidence(),
                    screenshotId
            );

            try {
                sessionService.disconnectSession(screenshot.getSessionId(),
                        "CRITICAL content violation: " +
                                (result.getCategories() != null ? String.join(", ", result.getCategories()) : "unknown"));
                log.warn("Session {} auto-disconnected due to CRITICAL violation", screenshot.getSessionId());
            } catch (Exception e) {
                log.error("Failed to disconnect session {}: {}", screenshot.getSessionId(), e.getMessage());
            }
        }

        return result;
    }

    /**
     * 批量检查最近未检查的截图
     */
    public void checkPendingScreenshots() {
        // Find screenshots that have no corresponding check log
        List<SessionScreenshot> allScreenshots = screenshotMapper.selectList(null);
        for (SessionScreenshot screenshot : allScreenshots) {
            LambdaQueryWrapper<ContentCheckLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ContentCheckLog::getScreenshotId, screenshot.getId());
            long count = contentCheckLogMapper.selectCount(wrapper);
            if (count == 0) {
                try {
                    checkScreenshot(screenshot.getId());
                } catch (Exception e) {
                    log.error("Failed to check screenshot {}: {}", screenshot.getId(), e.getMessage());
                }
            }
        }
    }
}
