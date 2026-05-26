package com.zncloud.device.config;

import com.zncloud.device.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 设备离线检测定时任务
 * 每30秒扫描一次，将超过3个心跳周期(90s)未上报心跳的设备自动切换为 OFFLINE
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfflineDetectionTask {

    private final DeviceService deviceService;

    /**
     * 每30秒执行一次离线检测
     */
    @Scheduled(fixedRate = 30000)
    public void checkOfflineDevices() {
        try {
            int offlineCount = deviceService.checkAndMarkOffline();
            if (offlineCount > 0) {
                log.info("离线检测完成，{} 台设备被标记为 OFFLINE", offlineCount);
            }
        } catch (Exception e) {
            log.error("离线检测执行异常", e);
        }
    }
}
