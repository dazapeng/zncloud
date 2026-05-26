package com.zncloud.session.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 内容检查服务接口
 */
public interface ContentCheckService {

    /**
     * 检查图片内容
     *
     * @param imageUrl MinIO中的图片对象路径
     * @return 检查结果
     */
    CheckResult checkImage(String imageUrl);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    class CheckResult {
        /** 是否通过检查 */
        private boolean pass;

        /** 风险等级 */
        private RiskLevel riskLevel;

        /** 违规分类 */
        private List<String> categories;

        /** 置信度 0.0 ~ 1.0 */
        private double confidence;

        /** 检查结果: PASS/FLAGGED/ERROR */
        private String result;
    }

    enum RiskLevel {
        NONE,
        LOW,
        HIGH,
        CRITICAL
    }
}
