package com.zncloud.schedule.strategy;

import com.zncloud.schedule.model.DeviceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 兜底策略
 * 默认策略无可用结果时，随机分配其他区域在线设备
 */
@Slf4j
@Component
public class FallbackStrategy implements AllocationStrategy {

    private static final Random RANDOM = new Random();

    @Override
    public List<DeviceDTO> allocate(AllocationCriteria criteria, List<DeviceDTO> allDevices) {
        // 筛选在线设备
        List<DeviceDTO> onlineDevices = allDevices.stream()
                .filter(d -> "ONLINE".equals(d.getStatus()))
                .collect(Collectors.toList());

        if (onlineDevices.isEmpty()) {
            log.warn("FallbackStrategy: 无在线设备可供兜底分配");
            return Collections.emptyList();
        }

        // 排除偏好区域内的设备（因为主策略已失败）
        String regionPreference = criteria.getRegionPreference();
        List<DeviceDTO> fallbackCandidates;
        if (regionPreference != null && !regionPreference.isBlank()) {
            fallbackCandidates = onlineDevices.stream()
                    .filter(d -> !regionPreference.equals(d.getCity())
                            && !regionPreference.equals(d.getProvince()))
                    .collect(Collectors.toList());
            // 如果其他区域也没有，就用全部在线设备
            if (fallbackCandidates.isEmpty()) {
                fallbackCandidates = onlineDevices;
            }
        } else {
            fallbackCandidates = onlineDevices;
        }

        // 随机打乱
        Collections.shuffle(fallbackCandidates, RANDOM);
        log.info("FallbackStrategy: 兜底分配，候选设备 {} 台", fallbackCandidates.size());
        return fallbackCandidates;
    }
}
