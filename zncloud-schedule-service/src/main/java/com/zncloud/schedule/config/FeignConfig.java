package com.zncloud.schedule.config;

import feign.Logger;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Feign 客户端配置
 */
@Configuration
public class FeignConfig {

    /**
     * Feign 日志级别 - 生产环境可改为 BASIC 或 NONE
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
