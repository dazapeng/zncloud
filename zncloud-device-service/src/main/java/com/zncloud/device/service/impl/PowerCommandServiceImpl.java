package com.zncloud.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zncloud.device.model.Device;
import com.zncloud.device.model.DeviceStatus;
import com.zncloud.device.model.dto.PendingCommand;
import com.zncloud.device.model.dto.PendingCommandResponse;
import com.zncloud.device.repository.DeviceMapper;
import com.zncloud.device.repository.PendingCommandMapper;
import com.zncloud.device.service.PowerCommandService;
import com.zncloud.device.util.WolUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 电源命令服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PowerCommandServiceImpl implements PowerCommandService {

    private final DeviceMapper deviceMapper;
    private final PendingCommandMapper pendingCommandMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean sendWakeOnLan(String deviceId) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            log.warn("发送 WoL 失败，设备不存在: {}", deviceId);
            return false;
        }

        String macAddress = device.getMacAddress();
        if (macAddress == null || macAddress.trim().isEmpty()) {
            log.warn("发送 WoL 失败，设备 {} 没有 MAC 地址", deviceId);
            return false;
        }

        // 发送 WoL 魔术包
        WolUtil.WolResult result = WolUtil.sendMagicPacket(macAddress);

        if (result.isSuccess()) {
            // 更新设备状态为 PENDING_ONLINE
            device.setStatus(DeviceStatus.PENDING_ONLINE);
            deviceMapper.updateById(device);
            log.info("WoL 发送成功, 设备 {} 状态已设为 PENDING_ONLINE", deviceId);
        } else {
            log.warn("WoL 发送失败, 设备 {}: {}", deviceId, result.getMessage());
        }

        return result.isSuccess();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PendingCommand createRebootCommand(String deviceId) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new RuntimeException("设备不存在: " + deviceId);
        }

        PendingCommand command = new PendingCommand();
        command.setDeviceId(deviceId);
        command.setType("REBOOT");
        command.setStatus("PENDING");
        command.setCreatedAt(LocalDateTime.now());
        pendingCommandMapper.insert(command);

        log.info("创建重启命令成功, 设备ID: {}, 命令ID: {}", deviceId, command.getId());
        return command;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PendingCommand createPoweroffCommand(String deviceId) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new RuntimeException("设备不存在: " + deviceId);
        }

        PendingCommand command = new PendingCommand();
        command.setDeviceId(deviceId);
        command.setType("POWEROFF");
        command.setStatus("PENDING");
        command.setCreatedAt(LocalDateTime.now());
        pendingCommandMapper.insert(command);

        log.info("创建关机命令成功, 设备ID: {}, 命令ID: {}", deviceId, command.getId());
        return command;
    }

    @Override
    public PendingCommandResponse getPendingCommand(String deviceId) {
        LambdaQueryWrapper<PendingCommand> wrapper = new LambdaQueryWrapper<PendingCommand>()
                .eq(PendingCommand::getDeviceId, deviceId)
                .eq(PendingCommand::getStatus, "PENDING")
                .orderByDesc(PendingCommand::getCreatedAt)
                .last("LIMIT 1");

        PendingCommand command = pendingCommandMapper.selectOne(wrapper);

        if (command == null) {
            return null;
        }

        return PendingCommandResponse.builder()
                .id(command.getId())
                .deviceId(command.getDeviceId())
                .type(command.getType())
                .status(command.getStatus())
                .createdAt(command.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acknowledgeCommand(Long commandId, String deviceId, String result, String message) {
        PendingCommand command = pendingCommandMapper.selectById(commandId);
        if (command == null) {
            throw new RuntimeException("命令不存在: " + commandId);
        }

        if (!command.getDeviceId().equals(deviceId)) {
            throw new RuntimeException("命令不属于该设备");
        }

        command.setStatus(result);
        command.setAcknowledgedAt(LocalDateTime.now());
        command.setResultMessage(message);
        pendingCommandMapper.updateById(command);

        log.info("命令已确认, 命令ID: {}, 设备ID: {}, 结果: {}, 消息: {}", commandId, deviceId, result, message);
    }

    @Override
    public List<PendingCommand> getCommandsByDeviceId(String deviceId) {
        LambdaQueryWrapper<PendingCommand> wrapper = new LambdaQueryWrapper<PendingCommand>()
                .eq(PendingCommand::getDeviceId, deviceId)
                .orderByDesc(PendingCommand::getCreatedAt);

        return pendingCommandMapper.selectList(wrapper);
    }
}
