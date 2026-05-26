package com.zncloud.device.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zncloud.device.model.ApiResult;
import com.zncloud.device.model.vo.*;
import com.zncloud.device.service.OperationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/operations")
@RequiredArgsConstructor
public class OperationsController {

    private final OperationsService operationsService;

    /**
     * 区域统计
     */
    @GetMapping("/stats/region")
    public ApiResult<List<OperatorStatsVO>> getRegionStats(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String isp,
            @RequestParam(required = false) String configLevel,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        List<OperatorStatsVO> stats = operationsService.getRegionStats(
                province, city, isp, configLevel, minPrice, maxPrice);
        return ApiResult.success(stats);
    }

    /**
     * ISP统计
     */
    @GetMapping("/stats/isp")
    public ApiResult<List<IspStatsVO>> getIspStats(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String isp,
            @RequestParam(required = false) String configLevel,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        List<IspStatsVO> stats = operationsService.getIspStats(
                province, city, isp, configLevel, minPrice, maxPrice);
        return ApiResult.success(stats);
    }

    /**
     * 配置等级统计
     */
    @GetMapping("/stats/config")
    public ApiResult<List<ConfigStatsVO>> getConfigStats(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String isp,
            @RequestParam(required = false) String configLevel,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        List<ConfigStatsVO> stats = operationsService.getConfigStats(
                province, city, isp, configLevel, minPrice, maxPrice);
        return ApiResult.success(stats);
    }

    /**
     * 价格区间统计
     */
    @GetMapping("/stats/price-range")
    public ApiResult<List<PriceRangeStatsVO>> getPriceRangeStats(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String isp,
            @RequestParam(required = false) String configLevel,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        List<PriceRangeStatsVO> stats = operationsService.getPriceRangeStats(
                province, city, isp, configLevel, minPrice, maxPrice);
        return ApiResult.success(stats);
    }

    /**
     * 运营视图设备列表（分页）
     */
    @GetMapping("/devices")
    public ApiResult<IPage<OperatorDeviceVO>> getOperatorDevices(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String isp,
            @RequestParam(required = false) String configLevel,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        IPage<OperatorDeviceVO> result = operationsService.getOperatorDevices(
                province, city, isp, configLevel, minPrice, maxPrice, page, size);
        return ApiResult.success(result);
    }

    /**
     * 获取所有可用筛选选项
     */
    @GetMapping("/filters")
    public ApiResult<Map<String, Object>> getFilterOptions() {
        Map<String, Object> options = operationsService.getFilterOptions();
        return ApiResult.success(options);
    }

    /**
     * 批量变更设备在线/离线状态
     */
    @PostMapping("/batch/status")
    public ApiResult<Integer> batchChangeStatus(@Valid @RequestBody BatchOnlineRequest request) {
        int affectedCount = operationsService.batchChangeStatus(request);
        return ApiResult.success(affectedCount);
    }

    /**
     * 批量调整设备价格（需要OPERATOR或SUPER_ADMIN角色）
     */
    @PostMapping("/batch/price")
    public ApiResult<Integer> batchAdjustPrice(
            @Valid @RequestBody BatchPriceChangeRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) Long operatorId) {
        int affectedCount = operationsService.batchAdjustPrice(request, operatorId);
        return ApiResult.success(affectedCount);
    }

    /**
     * 发布通知
     */
    @PostMapping("/notifications")
    public ApiResult<Long> publishNotification(
            @Valid @RequestBody NotificationRequest request,
            @RequestHeader(value = "X-Publisher-Id", required = false) Long publisherId) {
        Long notificationId = operationsService.publishNotification(request, publisherId);
        return ApiResult.success(notificationId);
    }

    /**
     * 分页查询通知列表
     */
    @GetMapping("/notifications")
    public ApiResult<IPage<Map<String, Object>>> listNotifications(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        IPage<Map<String, Object>> result = operationsService.listNotifications(type, page, size);
        return ApiResult.success(result);
    }

    /**
     * 导出设备数据为CSV
     */
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String isp,
            @RequestParam(required = false) String configLevel,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        // 导出最多10000条
        IPage<OperatorDeviceVO> devicePage = operationsService.getOperatorDevices(
                province, city, isp, configLevel, minPrice, maxPrice, 1, 10000);
        List<OperatorDeviceVO> devices = devicePage.getRecords();

        // 构建CSV内容
        StringBuilder csv = new StringBuilder();
        csv.append("设备ID,网吧ID,网吧名称,省份,城市,区县,运营商,配置等级,每小时价格,状态,最后在线时间,累计在线时长,累计收益,CPU信息,GPU信息,内存(GB)\n");

        for (OperatorDeviceVO device : devices) {
            csv.append(escapeCsv(device.getId())).append(",");
            csv.append(escapeCsv(device.getCafeId())).append(",");
            csv.append(escapeCsv(device.getCafeName())).append(",");
            csv.append(escapeCsv(device.getProvince())).append(",");
            csv.append(escapeCsv(device.getCity())).append(",");
            csv.append(escapeCsv(device.getDistrict())).append(",");
            csv.append(escapeCsv(device.getIsp())).append(",");
            csv.append(escapeCsv(device.getConfigLevel())).append(",");
            csv.append(device.getPricePerHour() != null ? device.getPricePerHour() : "").append(",");
            csv.append(escapeCsv(device.getStatus())).append(",");
            csv.append(device.getLastOnlineAt() != null ? device.getLastOnlineAt().toString() : "").append(",");
            csv.append(device.getTotalOnlineHours() != null ? device.getTotalOnlineHours() : "").append(",");
            csv.append(device.getTotalEarnings() != null ? device.getTotalEarnings() : "").append(",");
            csv.append(escapeCsv(device.getCpuInfo())).append(",");
            csv.append(escapeCsv(device.getGpuInfo())).append(",");
            csv.append(device.getMemoryGb() != null ? device.getMemoryGb() : "").append("\n");
        }

        byte[] csvBytes = csv.toString().getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "devices_export.csv");
        headers.setContentLength(csvBytes.length);

        return ResponseEntity.ok().headers(headers).body(csvBytes);
    }

    /**
     * CSV字段转义：如果包含逗号、引号或换行符，用双引号包裹
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
