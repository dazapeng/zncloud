package com.zncloud.schedule.model;

import lombok.Data;

/**
 * 调度请求链路日志 - 记录完整调度过程
 */
@Data
public class ScheduleLog {

    /** 调度请求ID */
    private String requestId;

    /** 用户ID */
    private String userId;

    /** 筛选条件 */
    private String filterCriteria;

    /** 候选设备列表 (设备ID逗号分隔) */
    private String candidateDevices;

    /** 最终分配设备ID */
    private String allocatedDeviceId;

    /** 故障转移记录 */
    private String failoverRecords;

    /** 分配是否成功 */
    private Boolean success;

    /** 错误信息 */
    private String errorMessage;

    /** 创建时间 */
    private String createTime;
}
