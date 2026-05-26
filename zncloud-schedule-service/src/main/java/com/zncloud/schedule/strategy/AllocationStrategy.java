package com.zncloud.schedule.strategy;

import com.zncloud.schedule.model.DeviceDTO;

import java.util.List;

/**
 * 分配策略接口
 */
public interface AllocationStrategy {

    /**
     * 根据筛选条件分配设备
     *
     * @param criteria 分配条件
     * @param allDevices 所有候选设备列表
     * @return 分配结果设备列表（按优先级排序）
     */
    List<DeviceDTO> allocate(AllocationCriteria criteria, List<DeviceDTO> allDevices);
}
