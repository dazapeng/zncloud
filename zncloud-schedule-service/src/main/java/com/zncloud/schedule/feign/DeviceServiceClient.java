package com.zncloud.schedule.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * device-service 远程调用客户端
 *
 * 注意：device-service 的所有接口返回 ApiResult<T> 包装，
 * Feign 客户端返回 String 类型，由调用方自行解析 JSON
 */
@FeignClient(name = "zncloud-device-service", path = "/api/v1/devices")
public interface DeviceServiceClient {

    /**
     * 获取单个设备详情
     * 返回原始 JSON 字符串 (ApiResult<Device>)
     */
    @GetMapping("/{id}")
    String getDeviceById(@PathVariable("id") String id);

    /**
     * 获取在线设备数量
     * 返回原始 JSON 字符串 (ApiResult<Long>)
     */
    @GetMapping("/online-count")
    String getOnlineCount();

    /**
     * 获取设备列表（分页）
     * 返回原始 JSON 字符串 (ApiResult<IPage<Device>>)
     */
    @GetMapping
    String listDevices(@RequestParam(value = "status", required = false) String status,
                       @RequestParam(value = "cafeId", required = false) String cafeId,
                       @RequestParam(value = "configLevel", required = false) String configLevel,
                       @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                       @RequestParam(value = "pageSize", defaultValue = "1000") Integer pageSize);
}
