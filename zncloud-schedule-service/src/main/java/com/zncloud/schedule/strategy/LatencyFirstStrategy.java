package com.zncloud.schedule.strategy;

import com.zncloud.schedule.model.DeviceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 延迟优先策略（默认策略）
 * 按城市分组 → 同城市优先 → 按最后在线时间倒序
 */
@Slf4j
@Component
public class LatencyFirstStrategy implements AllocationStrategy {

    @Override
    public List<DeviceDTO> allocate(AllocationCriteria criteria, List<DeviceDTO> allDevices) {
        // 筛选在线设备
        List<DeviceDTO> onlineDevices = allDevices.stream()
                .filter(d -> "ONLINE".equals(d.getStatus()) || "IN_USE".equals(d.getStatus()))
                .collect(Collectors.toList());

        if (onlineDevices.isEmpty()) {
            log.warn("LatencyFirstStrategy: 无在线设备可供分配");
            return onlineDevices;
        }

        String regionPreference = criteria.getRegionPreference();
        if (regionPreference == null || regionPreference.isBlank()) {
            // 无区域偏好，直接按最后在线时间倒序
            onlineDevices.sort(Comparator.comparing(DeviceDTO::getLastOnlineAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            log.info("LatencyFirstStrategy: 无区域偏好，按最后在线时间倒序排列，候选 {}", onlineDevices.size());
            return onlineDevices;
        }

        // 按城市分组：优先匹配同城市
        Map<Boolean, List<DeviceDTO>> partitioned = onlineDevices.stream()
                .collect(Collectors.partitioningBy(d ->
                        regionPreference.equals(d.getCity()) || regionPreference.equals(d.getProvince())));

        List<DeviceDTO> sameRegion = partitioned.get(true);
        List<DeviceDTO> otherRegion = partitioned.get(false);

        // 同区域优先，都按最后在线时间倒序
        sameRegion.sort(Comparator.comparing(DeviceDTO::getLastOnlineAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        otherRegion.sort(Comparator.comparing(DeviceDTO::getLastOnlineAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        List<DeviceDTO> result = sameRegion;
        result.addAll(otherRegion);

        log.info("LatencyFirstStrategy: 同区域 {} 台, 其他区域 {} 台", sameRegion.size(), otherRegion.size());
        return result;
    }
}
