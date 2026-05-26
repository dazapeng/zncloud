package com.zncloud.schedule.strategy;

import com.zncloud.schedule.model.DeviceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 去重策略
 * 同用户 5 分钟内不重复分配同设备（Redis + TTL）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeduplicationStrategy implements AllocationStrategy {

    /** Redis 去重键前缀 */
    private static final String DEDUP_KEY_PREFIX = "schedule:dedup:user:";

    /** 去重时间窗口 (5分钟) */
    private static final long DEDUP_TTL_SECONDS = 300;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<DeviceDTO> allocate(AllocationCriteria criteria, List<DeviceDTO> allDevices) {
        String userId = criteria.getUserId();
        if (userId == null || userId.isBlank()) {
            return allDevices;
        }

        // 过滤掉 5 分钟内已分配过的设备
        List<DeviceDTO> filtered = allDevices.stream()
                .filter(d -> !isRecentlyAllocated(userId, d.getId()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            log.warn("DeduplicationStrategy: 用户 {} 所有候选设备均在去重窗口内，返回原始列表", userId);
            return allDevices;
        }

        log.info("DeduplicationStrategy: 用户 {} 去重后候选设备 {} -> {} 台",
                userId, allDevices.size(), filtered.size());
        return filtered;
    }

    /**
     * 检查设备是否刚分配过给此用户
     */
    public boolean isRecentlyAllocated(String userId, String deviceId) {
        String key = DEDUP_KEY_PREFIX + userId + ":" + deviceId;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    /**
     * 记录设备分配，加入去重缓存
     */
    public void markAllocated(String userId, String deviceId) {
        String key = DEDUP_KEY_PREFIX + userId + ":" + deviceId;
        stringRedisTemplate.opsForValue().set(key, "1", DEDUP_TTL_SECONDS, TimeUnit.SECONDS);
        log.debug("DeduplicationStrategy: 用户 {} 分配设备 {}，已加入去重缓存 (TTL={}s)",
                userId, deviceId, DEDUP_TTL_SECONDS);
    }
}
