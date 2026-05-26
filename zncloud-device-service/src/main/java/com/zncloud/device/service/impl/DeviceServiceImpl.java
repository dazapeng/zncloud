package com.zncloud.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zncloud.device.model.Cafe;
import com.zncloud.device.model.Device;
import com.zncloud.device.model.DeviceStatus;
import com.zncloud.device.repository.CafeMapper;
import com.zncloud.device.repository.DeviceMapper;
import com.zncloud.device.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {

    private final DeviceMapper deviceMapper;
    private final CafeMapper cafeMapper;

    @Value("${device.heartbeat.timeout:90}")
    private long heartbeatTimeout;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Device registerDevice(Device device) {
        // 检查MAC地址是否已存在
        if (StringUtils.hasText(device.getMacAddress())) {
            LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<Device>()
                    .eq(Device::getMacAddress, device.getMacAddress());
            Device existing = deviceMapper.selectOne(wrapper);

            if (existing != null) {
                // 复用已有设备ID，更新硬件信息
                existing.setCpuInfo(device.getCpuInfo());
                existing.setGpuInfo(device.getGpuInfo());
                existing.setMemoryGb(device.getMemoryGb());
                existing.setDiskGb(device.getDiskGb());
                existing.setOsVersion(device.getOsVersion());
                existing.setPublicIp(device.getPublicIp());
                existing.setConfigLevel(device.getConfigLevel());
                existing.setPricePerHour(device.getPricePerHour());
                existing.setCafeId(device.getCafeId());
                existing.setStatus(DeviceStatus.REGISTERED);
                existing.setLastOnlineAt(null);
                existing.setRegisteredAt(LocalDateTime.now());
                deviceMapper.updateById(existing);
                log.info("设备重复注册，复用已有设备ID: {}, MAC: {}", existing.getId(), device.getMacAddress());
                return existing;
            }
        }

        // 全新注册
        device.setStatus(DeviceStatus.REGISTERED);
        device.setRegisteredAt(LocalDateTime.now());
        device.setTotalOnlineHours(0.0);
        device.setTotalEarnings(BigDecimal.ZERO);
        deviceMapper.insert(device);
        log.info("设备注册成功, ID: {}, MAC: {}", device.getId(), device.getMacAddress());
        return device;
    }

    @Override
    public Device getDeviceById(String id) {
        return deviceMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Device updateStatus(String id, String status) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new RuntimeException("设备不存在: " + id);
        }

        DeviceStatus newStatus = DeviceStatus.fromValue(status);
        DeviceStatus currentStatus = device.getStatus();

        // 状态机校验
        validateStateTransition(currentStatus, newStatus);

        // 如果切换到 OFFLINE，计算本次在线时长和收益
        if (newStatus == DeviceStatus.OFFLINE && currentStatus != DeviceStatus.OFFLINE) {
            calculateOfflineStats(device);
        }

        // 如果切换到 ONLINE，记录开始在线时间
        if (newStatus == DeviceStatus.ONLINE) {
            device.setLastOnlineAt(LocalDateTime.now());
        }

        device.setStatus(newStatus);
        deviceMapper.updateById(device);
        log.info("设备状态更新, ID: {}, {} -> {}", id, currentStatus, newStatus);
        return device;
    }

    @Override
    public long getOnlineCount() {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<Device>()
                .in(Device::getStatus, DeviceStatus.ONLINE, DeviceStatus.IN_USE);
        return deviceMapper.selectCount(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStatus(List<String> ids, String status) {
        DeviceStatus newStatus = DeviceStatus.fromValue(status);
        List<Device> devices = deviceMapper.selectBatchIds(ids);

        for (Device device : devices) {
            if (newStatus == DeviceStatus.OFFLINE && device.getStatus() != DeviceStatus.OFFLINE) {
                calculateOfflineStats(device);
            }
            if (newStatus == DeviceStatus.ONLINE) {
                device.setLastOnlineAt(LocalDateTime.now());
            }
            device.setStatus(newStatus);
        }

        updateBatchById(devices);
        log.info("批量更新设备状态完成, IDs: {}, status: {}", ids, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void heartbeat(String deviceId) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new RuntimeException("设备不存在: " + deviceId);
        }

        DeviceStatus previousStatus = device.getStatus();
        device.setLastOnlineAt(LocalDateTime.now());
        device.setStatus(DeviceStatus.ONLINE);
        deviceMapper.updateById(device);
        log.debug("设备心跳, ID: {}, 状态: {} -> ONLINE", deviceId, previousStatus);
    }

    @Override
    public IPage<Device> queryDevices(DeviceStatus status, String cafeId,
                                      String configLevel, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<Device>()
                .eq(status != null, Device::getStatus, status)
                .eq(StringUtils.hasText(cafeId), Device::getCafeId, cafeId)
                .eq(StringUtils.hasText(configLevel), Device::getConfigLevel,
                        com.zncloud.device.model.ConfigLevel.fromValue(configLevel))
                .orderByDesc(Device::getCreateTime);

        Page<Device> page = new Page<>(pageNum != null ? pageNum : 1,
                pageSize != null ? pageSize : 20);
        IPage<Device> result = deviceMapper.selectPage(page, wrapper);

        // 为每个设备补充网吧信息（省份/城市/网吧名称）
        for (Device device : result.getRecords()) {
            if (StringUtils.hasText(device.getCafeId())) {
                Cafe cafe = cafeMapper.selectById(device.getCafeId());
                if (cafe != null) {
                    device.setProvince(cafe.getProvince());
                    device.setCity(cafe.getCity());
                    device.setCafeName(cafe.getName());
                }
            }
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int checkAndMarkOffline() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusSeconds(heartbeatTimeout);

        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<Device>()
                .in(Device::getStatus, DeviceStatus.ONLINE, DeviceStatus.IN_USE)
                .lt(Device::getLastOnlineAt, timeoutThreshold);

        List<Device> offlineDevices = deviceMapper.selectList(wrapper);

        for (Device device : offlineDevices) {
            calculateOfflineStats(device);
            device.setStatus(DeviceStatus.OFFLINE);
        }

        if (!offlineDevices.isEmpty()) {
            updateBatchById(offlineDevices);
            log.warn("离线检测: {} 台设备因心跳超时(>{}s)被设为OFFLINE",
                    offlineDevices.size(), heartbeatTimeout);
        }

        return offlineDevices.size();
    }

    @Override
    public List<Device> getDevicesByIds(List<String> ids) {
        return deviceMapper.selectBatchIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDevice(String id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new RuntimeException("设备不存在: " + id);
        }
        deviceMapper.deleteById(id);
        log.info("设备逻辑删除成功, ID: {}", id);
    }

    @Override
    public Cafe extractCafeInfo(String cafeId) {
        if (!StringUtils.hasText(cafeId)) {
            return null;
        }
        return cafeMapper.selectById(cafeId);
    }

    /**
     * 计算离线时的在线时长和收益
     */
    private void calculateOfflineStats(Device device) {
        if (device.getLastOnlineAt() != null && device.getStatus() == DeviceStatus.ONLINE) {
            // 计算本次在线时长（分钟）
            long minutesOnline = java.time.Duration.between(device.getLastOnlineAt(), LocalDateTime.now())
                    .toMinutes();
            if (minutesOnline > 0) {
                double hoursOnline = minutesOnline / 60.0;
                double totalHours = (device.getTotalOnlineHours() != null ? device.getTotalOnlineHours() : 0.0) + hoursOnline;
                device.setTotalOnlineHours(totalHours);

                if (device.getPricePerHour() != null) {
                    BigDecimal earnings = device.getPricePerHour()
                            .multiply(BigDecimal.valueOf(hoursOnline));
                    BigDecimal total = (device.getTotalEarnings() != null ? device.getTotalEarnings() : BigDecimal.ZERO)
                            .add(earnings);
                    device.setTotalEarnings(total);
                }
            }
        }
    }

    /**
     * 状态机校验
     * REGISTERED → ONLINE → IN_USE → ONLINE → OFFLINE
     * OFFLINE → PENDING_ONLINE → ONLINE
     * PENDING_ONLINE → OFFLINE
     * REGISTERED → (直接跳转)
     */
    private void validateStateTransition(DeviceStatus current, DeviceStatus target) {
        if (current == target) {
            return; // 相同状态允许
        }

        boolean valid = switch (current) {
            case REGISTERED -> target == DeviceStatus.ONLINE;
            case ONLINE -> target == DeviceStatus.IN_USE || target == DeviceStatus.OFFLINE;
            case IN_USE -> target == DeviceStatus.ONLINE || target == DeviceStatus.OFFLINE;
            case OFFLINE -> target == DeviceStatus.ONLINE || target == DeviceStatus.REGISTERED
                    || target == DeviceStatus.PENDING_ONLINE;
            case PENDING_ONLINE -> target == DeviceStatus.ONLINE || target == DeviceStatus.OFFLINE
                    || target == DeviceStatus.REGISTERED;
        };

        if (!valid) {
            throw new IllegalStateException(
                    String.format("非法状态转换: %s -> %s", current, target));
        }
    }
}
