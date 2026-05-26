package com.zncloud.device.model.vo;

import com.zncloud.device.model.DeviceStatus;
import com.zncloud.device.model.ConfigLevel;
import lombok.Data;

@Data
public class DeviceQueryVO {

    /** 按状态筛选 */
    private DeviceStatus status;

    /** 按网吧ID筛选 */
    private String cafeId;

    /** 按配置等级筛选 */
    private ConfigLevel configLevel;

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页大小 */
    private Integer pageSize = 20;
}
