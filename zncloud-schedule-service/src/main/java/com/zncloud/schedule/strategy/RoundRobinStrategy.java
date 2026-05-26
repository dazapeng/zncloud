package com.zncloud.schedule.strategy;

import com.zncloud.schedule.model.DeviceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 轮询策略
 * 同网吧同配置的设备轮流分配（用 Redis 计数器实现轮询下标）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoundRobinStrategy implements AllocationStrategy {

    private static final String RR_COUNTER_KEY = "schedule:rr:counter:%s:%s";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<DeviceDTO> allocate(AllocationCriteria criteria, List<DeviceDTO> allDevices) {
        // 按网吧+配置分组
        Map<String, List<DeviceDTO>> grouped = allDevices.stream()
                .filter(d -> "ONLINE".equals(d.getStatus()) || "IN_USE".equals(d.getStatus()))
                .collect(Collectors.groupingBy(d -> d.getCafeId() + ":" + d.getConfigLevel()));

        if (grouped.isEmpty()) {
            log.warn("RoundRobinStrategy: 无匹配设备");
            return allDevices;
        }

        // 优先选择同配置的设备组
        String targetConfig = criteria.getConfigPreference();

        // 按配置匹配度排序：优先选择符合配置偏好的组
        List<Map.Entry<String, List<DeviceDTO>>> sortedGroups = grouped.entrySet().stream()
                .sorted((e1, e2) -> {
                    String config1 = e1.getKey().split(":")[1];
                    String config2 = e2.getKey().split(":")[1];
                    int match1 = targetConfig != null && targetConfig.equals(config1) ? 0 : 1;
                    int match2 = targetConfig != null && targetConfig.equals(config2) ? 0 : 1;
                    return Integer.compare(match1, match2);
                })
                .collect(Collectors.toList());

        // 对每组内的设备进行轮询排序
        for (Map.Entry<String, List<DeviceDTO>> entry : grouped.entrySet()) {
            String cafeId = entry.getKey().split(":")[0];
            String configLevel = entry.getKey().split(":")[1];
            List<DeviceDTO> devices = entry.getValue();

            // 获取轮询下标
            String counterKey = String.format(RR_COUNTER_KEY, cafeId, configLevel);
            Long index = stringRedisTemplate.opsForValue().increment(counterKey);
            if (index == null) {
                index = 0L;
            }

            // 让下标循环
            int idx = (int) (index % devices.size());
            // 通过轮询下标来调整列表顺序：将选中的设备放最前面
            DeviceDTO selected = devices.get(idx);
            devices.remove(idx);
            devices.add(0, selected);

            log.debug("RoundRobinStrategy: cafe={}, config={}, index={}, selected={}",
                    cafeId, configLevel, idx, selected.getId());
        }

        // 合并结果
        List<DeviceDTO> result = sortedGroups.stream()
                .flatMap(e -> grouped.get(e.getKey()).stream())
                .collect(Collectors.toList());

        log.info("RoundRobinStrategy: 轮询排序完成，候选 {} 台", result.size());
        return result;
    }
}
