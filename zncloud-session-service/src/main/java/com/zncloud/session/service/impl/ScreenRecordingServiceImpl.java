package com.zncloud.session.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zncloud.session.model.entity.ScreenRecording;
import com.zncloud.session.repository.ScreenRecordingMapper;
import com.zncloud.session.service.ScreenRecordingService;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenRecordingServiceImpl implements ScreenRecordingService {

    private final ScreenRecordingMapper screenRecordingMapper;
    private final MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScreenRecording completeRecording(String sessionId) {
        String mergedFilePath = mergeSegments(sessionId);

        // 更新所有分段状态为 COMPLETED
        LambdaQueryWrapper<ScreenRecording> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScreenRecording::getSessionId, sessionId)
                .eq(ScreenRecording::getMerged, false);

        ScreenRecording updateEntity = new ScreenRecording();
        updateEntity.setMerged(true);
        updateEntity.setMergedFilePath(mergedFilePath);
        updateEntity.setStatus("COMPLETED");
        screenRecordingMapper.update(updateEntity, wrapper);

        log.info("Screen recording completed for session: {}, merged file: {}", sessionId, mergedFilePath);

        // 返回第一条记录作为代表
        LambdaQueryWrapper<ScreenRecording> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ScreenRecording::getSessionId, sessionId)
                .orderByAsc(ScreenRecording::getSegmentNumber)
                .last("LIMIT 1");
        return screenRecordingMapper.selectOne(queryWrapper);
    }

    @Override
    public String mergeSegments(String sessionId) {
        try {
            // 获取该会话的所有录制分段，按分段编号排序
            LambdaQueryWrapper<ScreenRecording> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ScreenRecording::getSessionId, sessionId)
                    .eq(ScreenRecording::getStatus, "RECORDING")
                    .orderByAsc(ScreenRecording::getSegmentNumber);
            List<ScreenRecording> segments = screenRecordingMapper.selectList(wrapper);

            if (segments.isEmpty()) {
                log.warn("No recording segments found for session: {}", sessionId);
                return null;
            }

            // 下载所有分段并合并
            ByteArrayOutputStream mergedOutput = new ByteArrayOutputStream();
            for (ScreenRecording segment : segments) {
                try (InputStream is = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(segment.getFilePath())
                                .build())) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        mergedOutput.write(buffer, 0, bytesRead);
                    }
                } catch (Exception e) {
                    log.error("Failed to download segment {} for session {}: {}",
                            segment.getSegmentNumber(), sessionId, e.getMessage());
                    // Mark this segment as failed
                    segment.setStatus("FAILED");
                    screenRecordingMapper.updateById(segment);
                    throw e;
                }
            }

            // 上传合并后的文件到MinIO
            String mergedFileName = "screen-recordings/" + sessionId + "/merged.mp4";
            byte[] mergedBytes = mergedOutput.toByteArray();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(mergedFileName)
                            .stream(new java.io.ByteArrayInputStream(mergedBytes), mergedBytes.length, -1)
                            .contentType("video/mp4")
                            .build());

            log.info("Merged file uploaded to MinIO: {}/{} ({} bytes)", bucketName, mergedFileName, mergedBytes.length);

            return mergedFileName;
        } catch (Exception e) {
            log.error("Failed to merge segments for session {}: {}", sessionId, e.getMessage(), e);
            throw new RuntimeException("Failed to merge screen recording segments", e);
        }
    }

    @Override
    public String getDownloadUrl(String sessionId) {
        try {
            // 查找该会话的合并文件路径
            LambdaQueryWrapper<ScreenRecording> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ScreenRecording::getSessionId, sessionId)
                    .eq(ScreenRecording::getMerged, true)
                    .isNotNull(ScreenRecording::getMergedFilePath)
                    .last("LIMIT 1");
            ScreenRecording recording = screenRecordingMapper.selectOne(wrapper);

            if (recording == null || recording.getMergedFilePath() == null) {
                // Fallback: try to merge if not yet merged
                String mergedFilePath = mergeSegments(sessionId);
                if (mergedFilePath == null) {
                    throw new RuntimeException("No recording found for session: " + sessionId);
                }
                // Generate download URL for the merged file
                return minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(bucketName)
                                .object(mergedFilePath)
                                .expiry(1, TimeUnit.HOURS)
                                .build());
            }

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(recording.getMergedFilePath())
                            .expiry(1, TimeUnit.HOURS)
                            .build());
        } catch (Exception e) {
            log.error("Failed to get download URL for session {}: {}", sessionId, e.getMessage(), e);
            throw new RuntimeException("Failed to get download URL", e);
        }
    }
}
