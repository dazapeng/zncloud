package com.zncloud.device.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zncloud.device.model.ApiResult;
import com.zncloud.device.model.Device;
import com.zncloud.device.model.DeviceStatus;
import com.zncloud.device.model.vo.BatchStatusRequest;
import com.zncloud.device.model.vo.DeviceQueryVO;
import com.zncloud.device.model.vo.HeartbeatRequest;
import com.zncloud.device.model.vo.StatusUpdateRequest;
import com.zncloud.device.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * 注册设备
     * 相同MAC地址重复注册复用已有设备ID并更新硬件信息
     */
    @PostMapping("/register")
    public ApiResult<Device> registerDevice(@Valid @RequestBody Device device) {
        Device registered = deviceService.registerDevice(device);
        return ApiResult.success(registered);
    }

    /**
     * 分页查询设备列表
     * 支持按状态/网吧/配置筛选
     */
    @GetMapping
    public ApiResult<IPage<Device>> listDevices(DeviceQueryVO queryVO) {
        IPage<Device> page = deviceService.queryDevices(
                queryVO.getStatus(),
                queryVO.getCafeId(),
                queryVO.getConfigLevel() != null ? queryVO.getConfigLevel().getValue() : null,
                queryVO.getPageNum(),
                queryVO.getPageSize()
        );
        return ApiResult.success(page);
    }

    /**
     * 根据ID获取设备详情
     */
    @GetMapping("/{id}")
    public ApiResult<Device> getDeviceById(@PathVariable("id") String id) {
        Device device = deviceService.getDeviceById(id);
        if (device == null) {
            return ApiResult.notFound("设备不存在: " + id);
        }
        return ApiResult.success(device);
    }

    /**
     * 获取在线设备数量
     */
    @GetMapping("/online-count")
    public ApiResult<Long> getOnlineCount() {
        long count = deviceService.getOnlineCount();
        return ApiResult.success(count);
    }

    /**
     * 更新设备状态（含状态机校验）
     */
    @PatchMapping("/{id}/status")
    public ApiResult<Device> updateStatus(@PathVariable("id") String id,
                                          @Valid @RequestBody StatusUpdateRequest request) {
        try {
            Device device = deviceService.updateStatus(id, request.getStatus());
            return ApiResult.success(device);
        } catch (IllegalStateException e) {
            return ApiResult.badRequest(e.getMessage());
        } catch (RuntimeException e) {
            return ApiResult.notFound(e.getMessage());
        }
    }

    /**
     * 批量更新设备状态
     */
    @PostMapping("/batch")
    public ApiResult<Void> batchUpdateStatus(@Valid @RequestBody BatchStatusRequest request) {
        deviceService.batchUpdateStatus(request.getDeviceIds(), request.getStatus());
        return ApiResult.success();
    }

    /**
     * 逻辑删除设备
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> deleteDevice(@PathVariable("id") String id) {
        try {
            deviceService.deleteDevice(id);
            return ApiResult.success();
        } catch (RuntimeException e) {
            return ApiResult.notFound(e.getMessage());
        }
    }

    /**
     * 设备心跳
     */
    @PostMapping("/heartbeat")
    public ApiResult<Void> heartbeat(@Valid @RequestBody HeartbeatRequest request) {
        try {
            deviceService.heartbeat(request.getDeviceId());
            return ApiResult.success();
        } catch (RuntimeException e) {
            return ApiResult.notFound(e.getMessage());
        }
    }
}
