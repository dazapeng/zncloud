package com.zncloud.device.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class BatchPriceChangeRequest {

    /** 操作类型：FILTER（按条件筛选）或 ALL（全部设备） */
    private String actionType;

    /** 筛选条件键值对，如 province=广东, city=深圳, isp=CHINA_TELECOM, configLevel=MAINSTREAM */
    private Map<String, String> filterCriteria;

    /** 调价方式：FIXED（固定值）或 PERCENTAGE（百分比） */
    private String adjustmentType;

    /** 调价数值：FIXED时直接设为该价格，PERCENTAGE时在原价基础上增加百分比（可为负值） */
    private BigDecimal adjustmentValue;
}
