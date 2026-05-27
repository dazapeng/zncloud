package com.zncloud.session.controller;

import com.zncloud.session.model.entity.SessionScreenshot;
import com.zncloud.session.model.vo.ScreenshotUploadRequest;
import com.zncloud.session.service.ContentCheckProcessingService;
import com.zncloud.session.service.ContentCheckService.CheckResult;
import com.zncloud.session.service.ScreenshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/sessions/screenshots")
@RequiredArgsConstructor
public class ScreenshotController {

    private final ScreenshotService screenshotService;
    private final ContentCheckProcessingService checkProcessingService;

    /**
     * 上传截图并进行内容检查
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadScreenshot(@RequestBody ScreenshotUploadRequest request) {
        try {
            // Decode base64 image
            byte[] imageData = Base64.getDecoder().decode(request.getImageData());
            String fileName = request.getFileName() != null ? request.getFileName() :
                    UUID.randomUUID().toString() + ".jpg";
            int fileSize = request.getFileSize() != null ? request.getFileSize() : imageData.length;

            // Upload to MinIO
            SessionScreenshot screenshot = screenshotService.uploadScreenshot(
                    request.getSessionId(), imageData, fileName, fileSize);

            // Perform content check
            CheckResult checkResult = checkProcessingService.checkScreenshot(screenshot.getId());

            log.info("Screenshot uploaded and checked: sessionId={}, screenshotId={}, pass={}",
                    request.getSessionId(), screenshot.getId(), checkResult.isPass());

            return ResponseEntity.ok(Map.of(
                    "screenshotId", screenshot.getId(),
                    "pass", checkResult.isPass(),
                    "riskLevel", checkResult.getRiskLevel().name(),
                    "confidence", checkResult.getConfidence()
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid base64 image data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid image data"));
        } catch (Exception e) {
            log.error("Failed to upload screenshot: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取截图URL
     */
    @GetMapping("/{screenshotId}/url")
    public ResponseEntity<Map<String, String>> getScreenshotUrl(@PathVariable Long screenshotId) {
        String url = screenshotService.getScreenshotUrl(screenshotId);
        if (url == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * 获取缩略图URL
     */
    @GetMapping("/{screenshotId}/thumbnail")
    public ResponseEntity<Map<String, String>> getThumbnailUrl(@PathVariable Long screenshotId) {
        String url = screenshotService.getThumbnailUrl(screenshotId);
        if (url == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * 重新检查截图
     */
    @PostMapping("/{screenshotId}/recheck")
    public ResponseEntity<Map<String, Object>> recheckScreenshot(@PathVariable Long screenshotId) {
        try {
            CheckResult result = checkProcessingService.checkScreenshot(screenshotId);
            return ResponseEntity.ok(Map.of(
                    "pass", result.isPass(),
                    "riskLevel", result.getRiskLevel().name(),
                    "confidence", result.getConfidence()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
