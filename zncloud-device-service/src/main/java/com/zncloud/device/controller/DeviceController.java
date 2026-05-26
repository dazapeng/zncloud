package com.zncloud.device.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zncloud.device.model.ApiResult;
import com.zncloud.device.model.Device;
import com.zncloud.device.model.DeviceStatus;
import com.zncloud.device.model.dto.PendingCommand;
import com.zncloud.device.model.dto.PendingCommandResponse;
import com.zncloud.device.model.dto.PowerCommandAckRequest;
import com.zncloud.device.model.vo.BatchStatusRequest;
import com.zncloud.device.model.vo.DeviceQueryVO;
import com.zncloud.device.model.vo.HeartbeatRequest;
import com.zncloud.device.model.vo.StatusUpdateRequest;
import com.zncloud.device.service.DeviceService;
import com.zncloud.device.service.PowerCommandService;
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
    private final PowerCommandService powerCommandService;

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

    // ========== 远程电源管理 ==========

    /**
     * 唤醒设备（发送 WoL 魔术包）
     */
    @PostMapping("/{id}/wake")
    public ApiResult<Void> wakeDevice(@PathVariable("id") String id) {
        Device device = deviceService.getDeviceById(id);
        if (device == null) {
            return ApiResult.notFound("设备不存在: " + id);
        }

        if (device.getStatus() != DeviceStatus.OFFLINE && device.getStatus() != DeviceStatus.REGISTERED) {
            return ApiResult.badRequest("设备不在离线状态，无法唤醒");
        }

        boolean success = powerCommandService.sendWakeOnLan(id);
        if (success) {
            return ApiResult.success();
        } else {
            return ApiResult.error("唤醒失败，请检查设备MAC地址配置");
        }
    }

    /**
     * 创建远程重启命令
     */
    @PostMapping("/{id}/reboot")
    public ApiResult<PendingCommand> rebootDevice(@PathVariable("id") String id) {
        Device device = deviceService.getDeviceById(id);
        if (device == null) {
            return ApiResult.notFound("设备不存在: " + id);
        }

        if (device.getStatus() != DeviceStatus.ONLINE && device.getStatus() != DeviceStatus.IN_USE) {
            return ApiResult.badRequest("只有在线设备才能执行重启操作");
        }

        try {
            PendingCommand command = powerCommandService.createRebootCommand(id);
            return ApiResult.success(command);
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    /**
     * 创建远程关机命令
     */
    @PostMapping("/{id}/poweroff")
    public ApiResult<PendingCommand> poweroffDevice(@PathVariable("id") String id) {
        Device device = deviceService.getDeviceById(id);
        if (device == null) {
            return ApiResult.notFound("设备不存在: " + id);
        }

        if (device.getStatus() != DeviceStatus.ONLINE && device.getStatus() != DeviceStatus.IN_USE) {
            return ApiResult.badRequest("只有在线设备才能执行关机操作");
        }

        try {
            PendingCommand command = powerCommandService.createPoweroffCommand(id);
            return ApiResult.success(command);
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    /**
     * 获取设备电源状态
     */
    @GetMapping("/{id}/power-status")
    public ApiResult<DeviceStatus> getPowerStatus(@PathVariable("id") String id) {
        Device device = deviceService.getDeviceById(id);
        if (device == null) {
            return ApiResult.notFound("设备不存在: " + id);
        }
        return ApiResult.success(device.getStatus());
    }

    /**
     * 获取设备待处理命令（客户端轮询）
     */
    @GetMapping("/{id}/pending-command")
    public ApiResult<PendingCommandResponse> getPendingCommand(@PathVariable("id") String id) {
        Device device = deviceService.getDeviceById(id);
        if (device == null) {
            return ApiResult.notFound("设备不存在: " + id);
        }

        PendingCommandResponse command = powerCommandService.getPendingCommand(id);
        if (command == null) {
            return ApiResult.success(null);
        }
        return ApiResult.success(command);
    }

    /**
     * 确认待处理命令（客户端上报执行结果）
     */
    @PostMapping("/pending-command/ack")
    public ApiResult<Void> acknowledgeCommand(@Valid @RequestBody PowerCommandAckRequest request) {
        try {
            powerCommandService.acknowledgeCommand(
                    request.getCommandId(),
                    request.getDeviceId(),
                    request.getResult(),
                    request.getMessage()
            );
            return ApiResult.success();
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    /**
     * 获取设备命令历史
     */
    @GetMapping("/{id}/commands")
    public ApiResult<List<PendingCommand>> getDeviceCommands(@PathVariable("id") String id) {
        Device device = deviceService.getDeviceById(id);
        if (device == null) {
            return ApiResult.notFound("设备不存在: " + id);
        }

        List<PendingCommand> commands = powerCommandService.getCommandsByDeviceId(id);
        return ApiResult.success(commands);
    }
}
