package com.zncloud.device.controller;

import com.zncloud.device.model.ApiResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Mock 数据接口
 * 提供模拟的网吧和设备硬件配置信息
 */
@Slf4j
@RestController
@RequestMapping("/mock")
@RequiredArgsConstructor
public class MockDataController {

    // ==================== 模拟网吧数据 ====================

    private static final List<MockCafe> MOCK_CAFES = new ArrayList<>();

    static {
        MOCK_CAFES.add(new MockCafe("cafe-001", "网鱼网咖(南京东路店)", "上海市", "上海市", "黄浦区", 45));
        MOCK_CAFES.add(new MockCafe("cafe-002", "杰拉网咖(西湖店)", "浙江省", "杭州市", "西湖区", 32));
        MOCK_CAFES.add(new MockCafe("cafe-003", "好特网咖(深圳湾店)", "广东省", "深圳市", "南山区", 28));
        MOCK_CAFES.add(new MockCafe("cafe-004", "绿树网咖(成都春熙店)", "四川省", "成都市", "锦江区", 36));
        MOCK_CAFES.add(new MockCafe("cafe-005", "爱尚网咖(天河店)", "广东省", "广州市", "天河区", 24));
        MOCK_CAFES.add(new MockCafe("cafe-006", "星巴克网咖(三里屯店)", "北京市", "北京市", "朝阳区", 40));
        MOCK_CAFES.add(new MockCafe("cafe-007", "先锋网咖(解放碑店)", "重庆市", "重庆市", "渝中区", 30));
        MOCK_CAFES.add(new MockCafe("cafe-008", "易购网咖(汉口店)", "湖北省", "武汉市", "江汉区", 22));
        MOCK_CAFES.add(new MockCafe("cafe-009", "飞鱼网咖(新街口店)", "江苏省", "南京市", "秦淮区", 35));
        MOCK_CAFES.add(new MockCafe("cafe-010", "天空网咖(五一广场店)", "湖南省", "长沙市", "芙蓉区", 18));
    }

    // ==================== 模拟硬件配置数据 ====================

    private static final Map<String, List<MockHardwareConfig>> MOCK_HARDWARE_CONFIGS = new LinkedHashMap<>();

    static {
        // cafe-001: 网鱼网咖
        MOCK_HARDWARE_CONFIGS.put("cafe-001", Arrays.asList(
                new MockHardwareConfig("ENTRY", "Intel i5-12400F", "NVIDIA RTX 3060", 16, 512, "Windows 11", 20),
                new MockHardwareConfig("MAINSTREAM", "Intel i7-13700F", "NVIDIA RTX 4060 Ti", 32, 1024, "Windows 11", 15),
                new MockHardwareConfig("HIGH_PERFORMANCE", "Intel i9-14900K", "NVIDIA RTX 4080 Super", 32, 2048, "Windows 11", 10)
        ));

        // cafe-002: 杰拉网咖
        MOCK_HARDWARE_CONFIGS.put("cafe-002", Arrays.asList(
                new MockHardwareConfig("ENTRY", "AMD R5 5600", "NVIDIA RTX 3050", 16, 512, "Windows 10", 15),
                new MockHardwareConfig("MAINSTREAM", "AMD R7 7800X3D", "NVIDIA RTX 4070", 32, 1024, "Windows 11", 12),
                new MockHardwareConfig("HIGH_PERFORMANCE", "AMD R9 7950X", "NVIDIA RTX 4090", 64, 2048, "Windows 11", 5)
        ));

        // cafe-003: 好特网咖
        MOCK_HARDWARE_CONFIGS.put("cafe-003", Arrays.asList(
                new MockHardwareConfig("ENTRY", "Intel i5-11400F", "NVIDIA RTX 2060", 16, 512, "Windows 10", 12),
                new MockHardwareConfig("MAINSTREAM", "Intel i7-12700F", "NVIDIA RTX 3070", 32, 1024, "Windows 11", 10),
                new MockHardwareConfig("HIGH_PERFORMANCE", "Intel i9-13900K", "NVIDIA RTX 4080", 32, 2048, "Windows 11", 6)
        ));

        // cafe-004: 绿树网咖
        MOCK_HARDWARE_CONFIGS.put("cafe-004", Arrays.asList(
                new MockHardwareConfig("ENTRY", "AMD R5 3600", "NVIDIA GTX 1660 Super", 16, 256, "Windows 10", 18),
                new MockHardwareConfig("MAINSTREAM", "AMD R7 5800X", "NVIDIA RTX 3060 Ti", 32, 512, "Windows 11", 12),
                new MockHardwareConfig("HIGH_PERFORMANCE", "AMD R9 5950X", "NVIDIA RTX 3080", 32, 1024, "Windows 11", 6)
        ));

        // cafe-005: 爱尚网咖
        MOCK_HARDWARE_CONFIGS.put("cafe-005", Arrays.asList(
                new MockHardwareConfig("ENTRY", "Intel i3-12100F", "NVIDIA GTX 1650", 8, 256, "Windows 10", 10),
                new MockHardwareConfig("MAINSTREAM", "Intel i5-12400F", "NVIDIA RTX 3060", 16, 512, "Windows 11", 10),
                new MockHardwareConfig("HIGH_PERFORMANCE", "Intel i7-13700KF", "NVIDIA RTX 4070 Ti", 32, 1024, "Windows 11", 4)
        ));

        // cafe-006: 星巴克网咖
        MOCK_HARDWARE_CONFIGS.put("cafe-006", Arrays.asList(
                new MockHardwareConfig("ENTRY", "Intel i5-12400", "NVIDIA RTX 3050", 16, 512, "Windows 11", 18),
                new MockHardwareConfig("MAINSTREAM", "Intel i7-13700", "NVIDIA RTX 4060", 32, 1024, "Windows 11", 14),
                new MockHardwareConfig("HIGH_PERFORMANCE", "Intel i9-13900KS", "NVIDIA RTX 4090", 64, 2048, "Windows 11", 8)
        ));

        // cafe-007: 先锋网咖
        MOCK_HARDWARE_CONFIGS.put("cafe-007", Arrays.asList(
                new MockHardwareConfig("ENTRY", "AMD R5 5600G", "NVIDIA RTX 3060", 16, 512, "Windows 10", 14),
                new MockHardwareConfig("MAINSTREAM", "AMD R7 7700X", "NVIDIA RTX 4070", 32, 1024, "Windows 11", 10),
                new MockHardwareConfig("HIGH_PERFORMANCE", "AMD R9 7950X3D", "NVIDIA RTX 4090 D", 64, 2048, "Windows 11", 6)
        ));

        // cafe-008: 易购网咖
        MOCK_HARDWARE_CONFIGS.put("cafe-008", Arrays.asList(
                new MockHardwareConfig("ENTRY", "Intel i5-10400F", "NVIDIA GTX 1660 Ti", 16, 256, "Windows 10", 10),
                new MockHardwareConfig("MAINSTREAM", "Intel i5-13400F", "NVIDIA RTX 3060 Ti", 16, 512, "Windows 11", 8),
                new MockHardwareConfig("HIGH_PERFORMANCE", "Intel i7-13700K", "NVIDIA RTX 4070 Super", 32, 1024, "Windows 11", 4)
        ));

        // cafe-009: 飞鱼网咖
        MOCK_HARDWARE_CONFIGS.put("cafe-009", Arrays.asList(
                new MockHardwareConfig("ENTRY", "AMD R5 5500", "NVIDIA RTX 2060 Super", 16, 512, "Windows 10", 16),
                new MockHardwareConfig("MAINSTREAM", "AMD R7 5700X", "NVIDIA RTX 3070 Ti", 32, 1024, "Windows 11", 12),
                new MockHardwareConfig("HIGH_PERFORMANCE", "AMD R9 5900X", "NVIDIA RTX 3080 Ti", 32, 2048, "Windows 11", 7)
        ));

        // cafe-010: 天空网咖
        MOCK_HARDWARE_CONFIGS.put("cafe-010", Arrays.asList(
                new MockHardwareConfig("ENTRY", "Intel i3-10105F", "NVIDIA GTX 1650", 8, 256, "Windows 10", 8),
                new MockHardwareConfig("MAINSTREAM", "Intel i5-11400F", "NVIDIA RTX 2060", 16, 512, "Windows 11", 6),
                new MockHardwareConfig("HIGH_PERFORMANCE", "Intel i7-11700KF", "NVIDIA RTX 3070", 32, 1024, "Windows 11", 4)
        ));

        // 其他网吧使用通用默认配置
        for (MockCafe cafe : MOCK_CAFES) {
            MOCK_HARDWARE_CONFIGS.putIfAbsent(cafe.getId(), Arrays.asList(
                    new MockHardwareConfig("ENTRY", "Intel i5-12400F", "NVIDIA RTX 3060", 16, 512, "Windows 11", 10),
                    new MockHardwareConfig("MAINSTREAM", "Intel i7-13700F", "NVIDIA RTX 4060 Ti", 32, 1024, "Windows 11", 8),
                    new MockHardwareConfig("HIGH_PERFORMANCE", "Intel i9-14900K", "NVIDIA RTX 4080", 32, 2048, "Windows 11", 4)
            ));
        }
    }

    /**
     * 获取网吧列表
     * GET /mock/cafes
     */
    @GetMapping("/cafes")
    public ApiResult<List<MockCafe>> getCafes() {
        return ApiResult.success(MOCK_CAFES);
    }

    /**
     * 获取指定网吧的硬件配置列表
     * GET /mock/cafes/{cafeId}/hardware-configs
     */
    @GetMapping("/cafes/{cafeId}/hardware-configs")
    public ApiResult<List<MockHardwareConfig>> getHardwareConfigs(@PathVariable String cafeId) {
        List<MockHardwareConfig> configs = MOCK_HARDWARE_CONFIGS.get(cafeId);
        if (configs == null) {
            return ApiResult.notFound("网吧不存在: " + cafeId);
        }
        return ApiResult.success(configs);
    }

    // ==================== 内部模型 ====================

    @Data
    @AllArgsConstructor
    public static class MockCafe {
        private String id;
        private String name;
        private String province;
        private String city;
        private String district;
        private int deviceCount;
    }

    @Data
    @AllArgsConstructor
    public static class MockHardwareConfig {
        private String configLevel;
        private String cpuInfo;
        private String gpuInfo;
        private int memoryGb;
        private int diskGb;
        private String osVersion;
        private int deviceCount;
    }
}
