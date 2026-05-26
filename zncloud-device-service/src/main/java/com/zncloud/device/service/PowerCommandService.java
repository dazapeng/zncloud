package com.zncloud.device.service;

import com.zncloud.device.model.dto.PendingCommand;
import com.zncloud.device.model.dto.PendingCommandResponse;

import java.util.List;

/**
 * 电源命令服务接口
 * 管理设备的远程唤醒、重启和关机命令
 */
public interface PowerCommandService {

    /**
     * 发送 Wake-on-LAN 魔术包唤醒设备
     *
     * @param deviceId 设备ID
     * @return true 如果 WoL 发送成功
     */
    boolean sendWakeOnLan(String deviceId);

    /**
     * 创建待处理的重启命令
     *
     * @param deviceId 设备ID
     * @return 创建的待处理命令
     */
    PendingCommand createRebootCommand(String deviceId);

    /**
     * 创建待处理的关机命令
     *
     * @param deviceId 设备ID
     * @return 创建的待处理命令
     */
    PendingCommand createPoweroffCommand(String deviceId);

    /**
     * 获取设备待处理命令（客户端轮询）
     *
     * @param deviceId 设备ID
     * @return 待处理命令，如果没有则返回 null
     */
    PendingCommandResponse getPendingCommand(String deviceId);

    /**
     * 确认/清除待处理命令
     *
     * @param commandId 命令ID
     * @param deviceId 设备ID
     * @param result 执行结果: COMPLETED/FAILED
     * @param message 结果消息
     */
    void acknowledgeCommand(Long commandId, String deviceId, String result, String message);

    /**
     * 获取设备的命令历史
     *
     * @param deviceId 设备ID
     * @return 命令列表
     */
    List<PendingCommand> getCommandsByDeviceId(String deviceId);
}
