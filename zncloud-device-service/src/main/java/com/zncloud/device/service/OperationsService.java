package com.zncloud.device.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zncloud.device.model.vo.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface OperationsService {

    /**
     * 获取区域统计
     */
    List<OperatorStatsVO> getRegionStats(String province, String city, String isp,
                                         String configLevel, BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * 获取ISP统计
     */
    List<IspStatsVO> getIspStats(String province, String city, String isp,
                                  String configLevel, BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * 获取配置等级统计
     */
    List<ConfigStatsVO> getConfigStats(String province, String city, String isp,
                                        String configLevel, BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * 获取价格区间统计
     */
    List<PriceRangeStatsVO> getPriceRangeStats(String province, String city, String isp,
                                                String configLevel, BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * 获取运营视图设备列表（分页）
     */
    IPage<OperatorDeviceVO> getOperatorDevices(String province, String city, String isp,
                                                String configLevel, BigDecimal minPrice, BigDecimal maxPrice,
                                                Integer pageNum, Integer pageSize);

    /**
     * 批量变更设备在线/离线状态
     * @return 受影响设备数量
     */
    int batchChangeStatus(BatchOnlineRequest request);

    /**
     * 批量调整设备价格
     * @param request 调价请求
     * @param operatorId 操作人ID
     * @return 受影响设备数量
     */
    int batchAdjustPrice(BatchPriceChangeRequest request, Long operatorId);

    /**
     * 发布通知
     * @param request 通知请求
     * @param publisherId 发布人ID
     * @return 通知ID
     */
    Long publishNotification(NotificationRequest request, Long publisherId);

    /**
     * 获取所有可用筛选选项
     * @return Map包含 provinces, cities, isps, configLevels 等列表
     */
    Map<String, Object> getFilterOptions();

    /**
     * 分页查询通知列表
     */
    IPage<Map<String, Object>> listNotifications(String type, Integer pageNum, Integer pageSize);
}
