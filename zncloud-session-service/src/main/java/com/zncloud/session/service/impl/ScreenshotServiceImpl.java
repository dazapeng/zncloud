package com.zncloud.session.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zncloud.session.model.entity.SessionScreenshot;
import com.zncloud.session.repository.SessionScreenshotMapper;
import com.zncloud.session.service.ScreenshotService;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenshotServiceImpl implements ScreenshotService {

    private final SessionScreenshotMapper screenshotMapper;
    private final MinioClient minioClient;

    @Value("${minio.bucket-name:screen-recordings}")
    private String bucketName;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SessionScreenshot uploadScreenshot(String sessionId, byte[] imageData, String fileName, int fileSize) {
        // Enforce retention: keep last 100 non-flagged frames per session
        // Flagged screenshots are preserved separately — they are NOT subject to the 100-frame cap
        LambdaQueryWrapper<SessionScreenshot> nonFlaggedCountWrapper = new LambdaQueryWrapper<>();
        nonFlaggedCountWrapper.eq(SessionScreenshot::getSessionId, sessionId)
                .eq(SessionScreenshot::getFlagged, false);
        long nonFlaggedCount = screenshotMapper.selectCount(nonFlaggedCountWrapper);
        if (nonFlaggedCount >= 100) {
            // Delete oldest non-flagged screenshot only
            LambdaQueryWrapper<SessionScreenshot> oldestWrapper = new LambdaQueryWrapper<>();
            oldestWrapper.eq(SessionScreenshot::getSessionId, sessionId)
                    .eq(SessionScreenshot::getFlagged, false)
                    .orderByAsc(SessionScreenshot::getCreatedAt)
                    .last("LIMIT 1");
            SessionScreenshot oldest = screenshotMapper.selectOne(oldestWrapper);
            if (oldest != null) {
                try {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(oldest.getStoragePath())
                                    .build());
                    if (oldest.getThumbnailPath() != null) {
                        minioClient.removeObject(
                                RemoveObjectArgs.builder()
                                        .bucket(bucketName)
                                        .object(oldest.getThumbnailPath())
                                        .build());
                    }
                } catch (Exception e) {
                    log.warn("Failed to remove old screenshot from MinIO: {}", e.getMessage());
                }
                screenshotMapper.deleteById(oldest.getId());
            }
        }

        // Upload to MinIO
        String timestamp = String.valueOf(System.currentTimeMillis());
        String objectPath = "sessions/" + sessionId + "/screenshots/" + timestamp + "_" + fileName;
        String thumbnailPath = "sessions/" + sessionId + "/thumbnails/" + timestamp + "_" + fileName;

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .stream(new ByteArrayInputStream(imageData), imageData.length, -1)
                            .contentType("image/jpeg")
                            .build());

            // For MVP, thumbnail is the same image (no processing)
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(thumbnailPath)
                            .stream(new ByteArrayInputStream(imageData), imageData.length, -1)
                            .contentType("image/jpeg")
                            .build());

            log.info("Screenshot uploaded to MinIO: {}/{} ({} bytes)", bucketName, objectPath, imageData.length);
        } catch (Exception e) {
            log.error("Failed to upload screenshot to MinIO: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload screenshot", e);
        }

        // Save to database
        SessionScreenshot screenshot = new SessionScreenshot();
        screenshot.setSessionId(sessionId);
        screenshot.setStoragePath(objectPath);
        screenshot.setThumbnailPath(thumbnailPath);
        screenshot.setFileSize(fileSize);
        screenshot.setFlagged(false);
        screenshot.setCreatedAt(LocalDateTime.now());
        screenshotMapper.insert(screenshot);

        return screenshot;
    }

    @Override
    public String getScreenshotUrl(Long screenshotId) {
        SessionScreenshot screenshot = screenshotMapper.selectById(screenshotId);
        if (screenshot == null) {
            return null;
        }
        return generatePresignedUrl(screenshot.getStoragePath());
    }

    @Override
    public String getThumbnailUrl(Long screenshotId) {
        SessionScreenshot screenshot = screenshotMapper.selectById(screenshotId);
        if (screenshot == null) {
            return null;
        }
        String path = screenshot.getThumbnailPath() != null ? screenshot.getThumbnailPath() : screenshot.getStoragePath();
        return generatePresignedUrl(path);
    }

    @Override
    public void cleanupExpiredScreenshots() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(180);
        LambdaQueryWrapper<SessionScreenshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(SessionScreenshot::getCreatedAt, cutoff)
                .eq(SessionScreenshot::getFlagged, true);

        List<SessionScreenshot> expired = screenshotMapper.selectList(wrapper);
        for (SessionScreenshot screenshot : expired) {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(screenshot.getStoragePath())
                                .build());
                if (screenshot.getThumbnailPath() != null) {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(screenshot.getThumbnailPath())
                                    .build());
                }
            } catch (Exception e) {
                log.warn("Failed to remove expired screenshot {}: {}", screenshot.getId(), e.getMessage());
            }
            screenshotMapper.deleteById(screenshot.getId());
        }
        if (!expired.isEmpty()) {
            log.info("Cleaned up {} expired flagged screenshots", expired.size());
        }
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
