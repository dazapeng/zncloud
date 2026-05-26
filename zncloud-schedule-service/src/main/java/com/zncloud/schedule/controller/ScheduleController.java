package com.zncloud.schedule.controller;

import com.zncloud.schedule.model.AllocateRequest;
import com.zncloud.schedule.model.AllocateResponse;
import com.zncloud.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 调度引擎 REST 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * 设备分配接口
     *
     * POST /api/v1/schedule/allocate
     * 请求:
     * {
     *   "userId": "xxx",
     *   "regionPreference": "广东省/深圳市",
     *   "configPreference": "ENTRY|MAINSTREAM|HIGH_PERFORMANCE",
     *   "priceMin": 0,
     *   "priceMax": 100
     * }
     * 响应:
     * {
     *   "deviceId": "xxx",
     *   "cafeId": "xxx",
     *   "deviceIp": "xxx",
     *   "connectionToken": "xxx",
     *   "region": "广东省-深圳市",
     *   "configLevel": "ENTRY"
     * }
     */
    @PostMapping("/allocate")
    public ResponseEntity<Map<String, Object>> allocate(@Valid @RequestBody AllocateRequest request) {
        log.info("收到调度分配请求: userId={}, region={}, config={}",
                request.getUserId(), request.getRegionPreference(), request.getConfigPreference());

        try {
            AllocateResponse response = scheduleService.allocate(request);
            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "message", "success",
                    "data", response
            ));
        } catch (Exception e) {
            log.error("调度分配失败", e);
            return ResponseEntity.ok(Map.of(
                    "code", 500,
                    "message", e.getMessage(),
                    "data", null
            ));
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "Schedule Service is running",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
