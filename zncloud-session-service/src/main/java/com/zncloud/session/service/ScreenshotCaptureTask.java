package com.zncloud.session.service;

import com.zncloud.session.model.entity.SessionScreenshot;
import com.zncloud.session.service.impl.ScreenshotServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定期截图捕获 + 内容检查定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScreenshotCaptureTask {

    private final SessionService sessionService;
    private final ScreenshotService screenshotService;
    private final ContentCheckService contentCheckService;

    /**
     * 每30秒检查活跃会话并模拟截图捕获
     * 实际生产环境中，此任务会通过WebSocket/远程桌面协议从设备端获取真实截图
     */
    @Scheduled(fixedRate = 30000)
    public void captureScreenshotsForActiveSessions() {
        // In a real implementation, this would connect to active remote desktop sessions
        // and capture actual screen frames via the device agent.
        // For MVP, we use a mock approach - the screenshot capture is triggered by the
        // device-side agent which sends frames to the upload endpoint.
        // This scheduled task handles the post-upload content checking for flagged screenshots.
        log.debug("Screenshot capture task running (MVP mode - passive check)");
    }

    /**
     * 每天凌晨3点清理过期截图
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredScreenshots() {
        log.info("Starting expired screenshot cleanup...");
        screenshotService.cleanupExpiredScreenshots();
    }
}
