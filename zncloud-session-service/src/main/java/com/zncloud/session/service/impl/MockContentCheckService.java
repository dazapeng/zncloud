package com.zncloud.session.service.impl;

import com.zncloud.session.service.ContentCheckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Mock 内容检查服务实现 - MVP阶段始终返回通过
 */
@Slf4j
@Service
public class MockContentCheckService implements ContentCheckService {

    @Override
    public CheckResult checkImage(String imageUrl) {
        log.debug("Mock content check for image: {} - always passing", imageUrl);
        return CheckResult.builder()
                .pass(true)
                .riskLevel(RiskLevel.NONE)
                .categories(Collections.emptyList())
                .confidence(0.0)
                .result("PASS")
                .build();
    }
}
