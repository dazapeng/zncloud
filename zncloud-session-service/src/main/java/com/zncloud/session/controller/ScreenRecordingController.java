package com.zncloud.session.controller;

import com.zncloud.session.model.entity.ScreenRecording;
import com.zncloud.session.service.ScreenRecordingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/v1/screen-recordings")
@RequiredArgsConstructor
public class ScreenRecordingController {

    private final ScreenRecordingService screenRecordingService;

    /**
     * 获取合并后录像文件的下载URL
     *
     * @param sessionId 会话ID
     * @return 重定向到预签名下载URL
     */
    @GetMapping("/{sessionId}/download")
    public ResponseEntity<Void> downloadRecording(@PathVariable("sessionId") String sessionId) {
        try {
            String url = screenRecordingService.getDownloadUrl(sessionId);
            log.info("Redirecting to download URL for session: {}", sessionId);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, url)
                    .build();
        } catch (Exception e) {
            log.error("Failed to get download URL for session {}: {}", sessionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * 完成录制并合并分段
     *
     * @param sessionId 会话ID
     * @return 合并后的录制信息
     */
    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<ScreenRecording> completeRecording(@PathVariable("sessionId") String sessionId) {
        try {
            ScreenRecording recording = screenRecordingService.completeRecording(sessionId);
            return ResponseEntity.ok(recording);
        } catch (Exception e) {
            log.error("Failed to complete recording for session {}: {}", sessionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
