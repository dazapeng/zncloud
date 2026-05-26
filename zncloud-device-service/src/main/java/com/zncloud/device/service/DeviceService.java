package com.zncloud.device.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zncloud.device.model.Cafe;
import com.zncloud.device.model.Device;
import com.zncloud.device.model.DeviceStatus;

import java.util.List;

public interface DeviceService {

    /**
     * 注册设备
     * 相同MAC地址重复注册时复用已有设备ID并更新硬件信息
     */
    Device registerDevice(Device device);

    /**
     * 根据ID获取设备
     */
    Device getDeviceById(String id);

    /**
     * 更新设备状态（含状态机校验）
     */
    Device updateStatus(String id, String status);

    /**
     * 获取在线设备数量（ONLINE + IN_USE）
     */
    long getOnlineCount();

    /**
     * 批量更新设备状态
     */
    void batchUpdateStatus(List<String> ids, String status);

    /**
     * 设备心跳
     * 更新 last_online_at 并设置 status=ONLINE
     */
    void heartbeat(String deviceId);

    /**
     * 分页条件查询设备
     */
    IPage<Device> queryDevices(DeviceStatus status, String cafeId,
                               String configLevel, Integer pageNum, Integer pageSize);

    /**
     * 离线检测 - 将超3个心跳周期未上报的设备设为 OFFLINE
     * @return 被设为OFFLINE的设备数量
     */
    int checkAndMarkOffline();

    /**
     * 获取设备列表（按ID集合）
     */
    List<Device> getDevicesByIds(List<String> ids);

    /**
     * 逻辑删除设备
     */
    void deleteDevice(String id);

    /**
     * 根据网吧ID获取网吧信息
     */
    Cafe extractCafeInfo(String cafeId);
}
