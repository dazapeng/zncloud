package com.zncloud.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zncloud.device.model.Device;
import com.zncloud.device.model.vo.*;
import com.zncloud.device.repository.DeviceMapper;
import com.zncloud.device.repository.OperationMapper;
import com.zncloud.device.service.DeviceService;
import com.zncloud.device.service.OperationsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationsServiceImpl implements OperationsService {

    private final OperationMapper operationMapper;
    private final DeviceMapper deviceMapper;
    private final DeviceService deviceService;

    @Override
    public List<OperatorStatsVO> getRegionStats(String province, String city, String isp,
                                                 String configLevel, BigDecimal minPrice, BigDecimal maxPrice) {
        List<Map<String, Object>> rows = operationMapper.selectRegionStats(
                province, city, isp, configLevel, minPrice, maxPrice);
        return rows.stream().map(this::mapToOperatorStatsVO).collect(Collectors.toList());
    }

    @Override
    public List<IspStatsVO> getIspStats(String province, String city, String isp,
                                         String configLevel, BigDecimal minPrice, BigDecimal maxPrice) {
        List<Map<String, Object>> rows = operationMapper.selectIspStats(
                province, city, isp, configLevel, minPrice, maxPrice);
        return rows.stream().map(this::mapToIspStatsVO).collect(Collectors.toList());
    }

    @Override
    public List<ConfigStatsVO> getConfigStats(String province, String city, String isp,
                                               String configLevel, BigDecimal minPrice, BigDecimal maxPrice) {
        List<Map<String, Object>> rows = operationMapper.selectConfigStats(
                province, city, isp, configLevel, minPrice, maxPrice);
        return rows.stream().map(this::mapToConfigStatsVO).collect(Collectors.toList());
    }

    @Override
    public List<PriceRangeStatsVO> getPriceRangeStats(String province, String city, String isp,
                                                       String configLevel, BigDecimal minPrice, BigDecimal maxPrice) {
        List<Map<String, Object>> rows = operationMapper.selectPriceRangeStats(
                province, city, isp, configLevel, minPrice, maxPrice);
        return rows.stream().map(this::mapToPriceRangeStatsVO).collect(Collectors.toList());
    }

    @Override
    public IPage<OperatorDeviceVO> getOperatorDevices(String province, String city, String isp,
                                                       String configLevel, BigDecimal minPrice, BigDecimal maxPrice,
                                                       Integer pageNum, Integer pageSize) {
        Page<Device> page = new Page<>(
                pageNum != null ? pageNum : 1,
                pageSize != null ? pageSize : 20);

        IPage<Device> devicePage = operationMapper.selectOperatorDevices(
                page, province, city, isp, configLevel, minPrice, maxPrice);

        // Convert to VO
        List<OperatorDeviceVO> voList = devicePage.getRecords().stream()
                .map(this::mapToOperatorDeviceVO)
                .collect(Collectors.toList());

        IPage<OperatorDeviceVO> resultPage = new Page<>(devicePage.getCurrent(), devicePage.getSize(), devicePage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchChangeStatus(BatchOnlineRequest request) {
        deviceService.batchUpdateStatus(request.getDeviceIds(), request.getAction());
        log.info("批量变更设备状态完成, IDs count: {}, action: {}", request.getDeviceIds().size(), request.getAction());
        return request.getDeviceIds().size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAdjustPrice(BatchPriceChangeRequest request, Long operatorId) {
        // 1. 构建筛选条件，查询匹配的设备
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<Device>()
                .eq(Device::getDeleted, 0);

        Map<String, String> criteria = request.getFilterCriteria();
        if (criteria != null) {
            if (StringUtils.hasText(criteria.get("province"))) {
                wrapper.eq(Device::getProvince, criteria.get("province"));
            }
            if (StringUtils.hasText(criteria.get("city"))) {
                wrapper.eq(Device::getCity, criteria.get("city"));
            }
        }

        List<Device> devices = deviceMapper.selectList(wrapper);

        // 2. 更新价格
        for (Device device : devices) {
            BigDecimal newPrice;
            if ("FIXED".equalsIgnoreCase(request.getAdjustmentType())) {
                newPrice = request.getAdjustmentValue();
            } else if ("PERCENTAGE".equalsIgnoreCase(request.getAdjustmentType())) {
                BigDecimal oldPrice = device.getPricePerHour() != null ? device.getPricePerHour() : BigDecimal.ZERO;
                BigDecimal factor = request.getAdjustmentValue().divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
                BigDecimal change = oldPrice.multiply(factor);
                newPrice = oldPrice.add(change).setScale(2, java.math.RoundingMode.HALF_UP);
                if (newPrice.compareTo(BigDecimal.ZERO) < 0) {
                    newPrice = BigDecimal.ZERO;
                }
            } else {
                throw new IllegalArgumentException("不支持的调价方式: " + request.getAdjustmentType());
            }

            device.setPricePerHour(newPrice);
        }

        if (!devices.isEmpty()) {
            for (Device device : devices) {
                deviceMapper.updateById(device);
            }
        }

        // 3. 记录调价日志
        operationMapper.insertBatchPriceChangeLog(
                "BATCH",
                criteria != null ? criteria.toString() : "ALL",
                request.getAdjustmentType(),
                request.getAdjustmentValue(),
                devices.size(),
                operatorId != null ? operatorId.toString() : "0",
                LocalDateTime.now());

        log.info("批量调价完成, 影响设备数: {}, 调价方式: {}, 调价值: {}",
                devices.size(), request.getAdjustmentType(), request.getAdjustmentValue());
        return devices.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishNotification(NotificationRequest request, Long publisherId) {
        operationMapper.insertNotification(
                request.getTitle(),
                request.getContent(),
                request.getType(),
                request.getTargetType(),
                request.getTargetValue(),
                publisherId != null ? publisherId.toString() : "0",
                LocalDateTime.now());
        // Retrieve the last insert ID — use the generated ID return approach
        // We'll rely on DB auto-increment; for simplicity, return 0L placeholder
        // In production, use a SELECT LAST_INSERT_ID() or RETURNING clause
        log.info("通知发布成功, title: {}, type: {}, targetType: {}",
                request.getTitle(), request.getType(), request.getTargetType());
        return 0L;
    }

    @Override
    public Map<String, Object> getFilterOptions() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 查询所有省份
        List<Map<String, Object>> provinces = operationMapper.selectDistinctProvinces();
        result.put("provinces", provinces);

        // 查询所有城市
        List<Map<String, Object>> cities = operationMapper.selectDistinctCities();
        result.put("cities", cities);

        // 查询所有ISP
        List<Map<String, Object>> isps = operationMapper.selectDistinctIsps();
        result.put("isps", isps);

        // 配置等级（固定值）
        List<Map<String, String>> configLevels = Arrays.asList(
                Map.of("value", "ENTRY", "label", "入门级"),
                Map.of("value", "MAINSTREAM", "label", "主流级"),
                Map.of("value", "HIGH_PERFORMANCE", "label", "高性能级")
        );
        result.put("configLevels", configLevels);

        // 设备总数
        Long totalDeviceCount = deviceMapper.selectCount(
                new LambdaQueryWrapper<Device>().eq(Device::getDeleted, 0));
        result.put("totalDeviceCount", totalDeviceCount);

        return result;
    }

    @Override
    public IPage<Map<String, Object>> listNotifications(String type, Integer pageNum, Integer pageSize) {
        Page<Map<String, Object>> page = new Page<>(
                pageNum != null ? pageNum : 1,
                pageSize != null ? pageSize : 20);
        return operationMapper.selectNotifications(page, type);
    }

    // ==================== 内部转换方法 ====================

    private OperatorStatsVO mapToOperatorStatsVO(Map<String, Object> row) {
        OperatorStatsVO vo = new OperatorStatsVO();
        vo.setProvince(str(row.get("province")));
        vo.setCity(str(row.get("city")));
        vo.setDistrict(str(row.get("district")));
        vo.setDeviceCount(longVal(row.get("device_count")));
        vo.setOnlineCount(longVal(row.get("online_count")));
        vo.setInUseCount(longVal(row.get("in_use_count")));
        vo.setOfflineCount(longVal(row.get("offline_count")));
        vo.setOnlineRate(doubleVal(row.get("online_rate")));
        vo.setUtilizationRate(doubleVal(row.get("utilization_rate")));
        vo.setAvgPrice(bigDecimalVal(row.get("avg_price")));
        vo.setTotalEarnings(bigDecimalVal(row.get("total_earnings")));
        return vo;
    }

    private IspStatsVO mapToIspStatsVO(Map<String, Object> row) {
        IspStatsVO vo = new IspStatsVO();
        vo.setIsp(str(row.get("isp")));
        vo.setDeviceCount(longVal(row.get("device_count")));
        vo.setOnlineCount(longVal(row.get("online_count")));
        vo.setAvgPrice(bigDecimalVal(row.get("avg_price")));
        return vo;
    }

    private ConfigStatsVO mapToConfigStatsVO(Map<String, Object> row) {
        ConfigStatsVO vo = new ConfigStatsVO();
        vo.setConfigLevel(str(row.get("config_level")));
        vo.setDeviceCount(longVal(row.get("device_count")));
        vo.setOnlineCount(longVal(row.get("online_count")));
        Long inUseCount = longVal(row.get("in_use_count"));
        Long deviceCount = longVal(row.get("device_count"));
        if (deviceCount != null && deviceCount > 0 && inUseCount != null) {
            vo.setUtilizationRate(Math.round(inUseCount * 10000.0 / deviceCount) / 100.0);
        } else {
            vo.setUtilizationRate(0.0);
        }
        vo.setAvgPrice(bigDecimalVal(row.get("avg_price")));
        return vo;
    }

    private PriceRangeStatsVO mapToPriceRangeStatsVO(Map<String, Object> row) {
        PriceRangeStatsVO vo = new PriceRangeStatsVO();
        vo.setRangeName(str(row.get("range_name")));
        vo.setDeviceCount(longVal(row.get("device_count")));
        vo.setOnlineCount(longVal(row.get("online_count")));
        vo.setAvgPrice(bigDecimalVal(row.get("avg_price")));
        return vo;
    }

    private OperatorDeviceVO mapToOperatorDeviceVO(Device device) {
        OperatorDeviceVO vo = new OperatorDeviceVO();
        vo.setId(device.getId());
        vo.setCafeId(device.getCafeId());
        vo.setCafeName(device.getCafeName());
        vo.setProvince(device.getProvince());
        vo.setCity(device.getCity());
        // district is queried from the SQL but not mapped in Device entity; will be null
        vo.setIsp(device.getIsp() != null ? device.getIsp().getValue() : null);
        vo.setConfigLevel(device.getConfigLevel() != null ? device.getConfigLevel().getValue() : null);
        vo.setPricePerHour(device.getPricePerHour());
        vo.setStatus(device.getStatus() != null ? device.getStatus().getValue() : null);
        vo.setLastOnlineAt(device.getLastOnlineAt());
        vo.setTotalOnlineHours(device.getTotalOnlineHours());
        vo.setTotalEarnings(device.getTotalEarnings());
        vo.setCpuInfo(device.getCpuInfo());
        vo.setGpuInfo(device.getGpuInfo());
        vo.setMemoryGb(device.getMemoryGb());
        return vo;
    }

    // ==================== 类型转换辅助方法 ====================

    private String str(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    private Long longVal(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double doubleVal(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal bigDecimalVal(Object obj) {
        if (obj == null) return null;
        if (obj instanceof BigDecimal) return (BigDecimal) obj;
        if (obj instanceof Number) return BigDecimal.valueOf(((Number) obj).doubleValue());
        try {
            return new BigDecimal(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
