package com.zncloud.schedule.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zncloud.schedule.feign.DeviceServiceClient;
import com.zncloud.schedule.model.AllocateRequest;
import com.zncloud.schedule.model.AllocateResponse;
import com.zncloud.schedule.model.DeviceDTO;
import com.zncloud.schedule.model.ScheduleLog;
import com.zncloud.schedule.strategy.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 调度引擎核心服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final DeviceServiceClient deviceServiceClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private final LatencyFirstStrategy latencyFirstStrategy;
    private final FallbackStrategy fallbackStrategy;
    private final RoundRobinStrategy roundRobinStrategy;
    private final DeduplicationStrategy deduplicationStrategy;

    /** 调度日志 Redis key 前缀 */
    private static final String SCHEDULE_LOG_KEY = "schedule:log:";
    /** 调度日志 TTL (1小时) */
    private static final long SCHEDULE_LOG_TTL = 3600;

    /**
     * 执行设备分配
     */
    public AllocateResponse allocate(AllocateRequest request) {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        ScheduleLog scheduleLog = new ScheduleLog();
        scheduleLog.setRequestId(requestId);
        scheduleLog.setUserId(request.getUserId());
        scheduleLog.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        try {
            // 1. 构建筛选条件
            AllocationCriteria criteria = buildCriteria(request);
            scheduleLog.setFilterCriteria(toJson(criteria));

            // 2. 从 device-service 获取设备数据
            List<DeviceDTO> allDevices = fetchOnlineDevices();
            if (allDevices.isEmpty()) {
                throw new RuntimeException("没有可用的在线设备");
            }

            // 3. 根据配置偏好筛选
            List<DeviceDTO> filteredByConfig = filterByConfig(allDevices, criteria);
            if (filteredByConfig.isEmpty()) {
                filteredByConfig = allDevices;
            }

            // 4. 根据价格范围筛选
            List<DeviceDTO> filteredByPrice = filterByPrice(filteredByConfig, criteria);

            // 5. 记录候选列表
            List<DeviceDTO> candidates = filteredByPrice;
            scheduleLog.setCandidateDevices(
                    candidates.stream().map(DeviceDTO::getId).collect(Collectors.joining(",")));

            // 6. 应用去重策略
            synchronized (request.getUserId().intern()) {
                List<DeviceDTO> deduped = deduplicationStrategy.allocate(criteria, candidates);
                candidates = deduped;
            }

            // 7. 应用延迟优先策略（默认）
            List<DeviceDTO> prioritized = latencyFirstStrategy.allocate(criteria, candidates);

            // 8. 应用轮询策略
            List<DeviceDTO> rrOrdered = roundRobinStrategy.allocate(criteria, prioritized);

            // 9. 如果主策略无结果，使用兜底策略（随机分配其他区域在线设备）
            List<DeviceDTO> finalCandidates = rrOrdered;
            if (finalCandidates.isEmpty()) {
                log.warn("主策略无可用设备，触发兜底策略");
                finalCandidates = fallbackStrategy.allocate(criteria, allDevices);
                scheduleLog.setFailoverRecords("主策略无结果，使用兜底策略");
            }

            // 10. 选择设备并执行故障转移
            AllocateResponse response = allocateWithFailover(finalCandidates, criteria, scheduleLog);

            // 11. 记录分配结果
            scheduleLog.setAllocatedDeviceId(response != null ? response.getDeviceId() : null);
            scheduleLog.setSuccess(response != null);

            // 12. 记录去重
            if (response != null) {
                deduplicationStrategy.markAllocated(request.getUserId(), response.getDeviceId());
            }

            // 保存调度日志到 Redis
            saveScheduleLog(scheduleLog);

            if (response == null) {
                throw new RuntimeException("设备分配失败");
            }

            log.info("调度分配成功: requestId={}, userId={}, deviceId={}",
                    requestId, request.getUserId(), response.getDeviceId());
            return response;

        } catch (Exception e) {
            scheduleLog.setSuccess(false);
            scheduleLog.setErrorMessage(e.getMessage());
            saveScheduleLog(scheduleLog);
            log.error("调度分配失败: requestId={}, userId={}, error={}",
                    requestId, request.getUserId(), e.getMessage(), e);
            throw new RuntimeException("设备分配失败: " + e.getMessage());
        }
    }

    /**
     * 执行设备分配与故障转移
     */
    private AllocateResponse allocateWithFailover(List<DeviceDTO> candidates,
                                                   AllocationCriteria criteria,
                                                   ScheduleLog scheduleLog) {
        List<String> failoverRecords = new ArrayList<>();

        // 按网吧分组
        Map<String, List<DeviceDTO>> byCafe = candidates.stream()
                .collect(Collectors.groupingBy(DeviceDTO::getCafeId, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<DeviceDTO>> cafeEntry : byCafe.entrySet()) {
            String cafeId = cafeEntry.getKey();
            List<DeviceDTO> cafeDevices = cafeEntry.getValue();

            for (DeviceDTO device : cafeDevices) {
                try {
                    // 尝试分配此设备
                    // 这里模拟连接检查，实际场景中会调用设备连接验证
                    boolean connectionOk = simulateConnectionCheck(device);
                    if (connectionOk) {
                        return buildResponse(device);
                    } else {
                        failoverRecords.add("设备 " + device.getId() + " 连接失败");
                    }
                } catch (Exception e) {
                    failoverRecords.add("设备 " + device.getId() + " 异常: " + e.getMessage());
                }
            }

            // 同网吧所有设备都失败，尝试下一家网吧
            failoverRecords.add("网吧 " + cafeId + " 所有设备均不可用");
        }

        // 所有候选都失败
        scheduleLog.setFailoverRecords(String.join("; ", failoverRecords));
        return null;
    }

    /**
     * 构建分配响应
     */
    private AllocateResponse buildResponse(DeviceDTO device) {
        String region = (device.getProvince() != null ? device.getProvince() : "")
                + "-" + (device.getCity() != null ? device.getCity() : "");
        if (region.equals("-")) {
            region = "未知";
        }

        return AllocateResponse.builder()
                .deviceId(device.getId())
                .cafeId(device.getCafeId())
                .deviceIp(device.getPublicIp())
                .connectionToken(UUID.randomUUID().toString())
                .region(region)
                .configLevel(device.getConfigLevel())
                .build();
    }

    /**
     * 模拟设备连接检查
     */
    private boolean simulateConnectionCheck(DeviceDTO device) {
        // 模拟连接检查 - 在线设备默认可通过
        return "ONLINE".equals(device.getStatus());
    }

    /**
     * 从 device-service 获取在线设备
     */
    private List<DeviceDTO> fetchOnlineDevices() {
        try {
            String response = deviceServiceClient.listDevices("ONLINE", null, null, 1, 1000);
            // 解析返回的 ApiResult，从 data 字段提取设备列表
            // 由于 Feign 返回的是 String，用 ObjectMapper 手动解析
            return parseDeviceList(response);
        } catch (Exception e) {
            log.warn("Feign 调用 device-service 失败，尝试直接解析: {}", e.getMessage());
            // 兜底返回空列表
            return Collections.emptyList();
        }
    }

    /**
     * 解析 device-service 返回的设备列表
     */
    @SuppressWarnings("unchecked")
    private List<DeviceDTO> parseDeviceList(String response) {
        try {
            Map<String, Object> result = objectMapper.readValue(response, Map.class);
            int code = (int) result.getOrDefault("code", 500);
            if (code != 200) {
                log.warn("device-service 返回异常: code={}, msg={}", code, result.get("message"));
                return Collections.emptyList();
            }

            Object dataObj = result.get("data");
            if (dataObj == null) {
                return Collections.emptyList();
            }

            // device-service 返回的是 IPage 分页对象，data.records 里是设备列表
            if (dataObj instanceof Map) {
                Map<String, Object> pageData = (Map<String, Object>) dataObj;
                Object records = pageData.get("records");
                if (records instanceof List) {
                    return objectMapper.convertValue(records,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, DeviceDTO.class));
                }
            }

            // 直接尝试反序列化为 List
            return objectMapper.convertValue(dataObj,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, DeviceDTO.class));
        } catch (Exception e) {
            log.error("解析设备列表失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 构建筛选条件
     */
    private AllocationCriteria buildCriteria(AllocateRequest request) {
        return AllocationCriteria.builder()
                .userId(request.getUserId())
                .regionPreference(request.getRegionPreference())
                .configPreference(request.getConfigPreference())
                .priceMin(request.getPriceMin())
                .priceMax(request.getPriceMax())
                .build();
    }

    /**
     * 按配置偏好筛选
     */
    private List<DeviceDTO> filterByConfig(List<DeviceDTO> devices, AllocationCriteria criteria) {
        if (!StringUtils.hasText(criteria.getConfigPreference())) {
            return devices;
        }
        List<DeviceDTO> filtered = devices.stream()
                .filter(d -> criteria.getConfigPreference().equals(d.getConfigLevel()))
                .collect(Collectors.toList());
        log.debug("按配置筛选: {} -> {} 台", criteria.getConfigPreference(), filtered.size());
        return filtered;
    }

    /**
     * 按价格范围筛选
     */
    private List<DeviceDTO> filterByPrice(List<DeviceDTO> devices, AllocationCriteria criteria) {
        return devices.stream()
                .filter(d -> {
                    BigDecimal price = d.getPricePerHour();
                    if (price == null) return true;
                    if (criteria.getPriceMin() != null && price.compareTo(criteria.getPriceMin()) < 0) {
                        return false;
                    }
                    if (criteria.getPriceMax() != null && price.compareTo(criteria.getPriceMax()) > 0) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * 保存调度日志到 Redis
     */
    private void saveScheduleLog(ScheduleLog logEntry) {
        try {
            String key = SCHEDULE_LOG_KEY + logEntry.getRequestId();
            stringRedisTemplate.opsForValue().set(
                    key, toJson(logEntry), SCHEDULE_LOG_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("保存调度日志失败: {}", e.getMessage());
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj != null ? obj.toString() : "null";
        }
    }
}
