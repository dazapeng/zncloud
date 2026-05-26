package com.zncloud.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 用户服务 - 认证接口
                .route("user-service-auth", r -> r
                        .path("/api/v1/auth/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://zncloud-user-service"))
                // 用户服务 - 用户接口
                .route("user-service-users", r -> r
                        .path("/api/v1/users/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://zncloud-user-service"))
                // 管理后台 API（AK/SK 密钥管理 + Webhook 管理）
                .route("user-service-admin", r -> r
                        .path("/api/v1/admin/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://zncloud-user-service"))
                // 设备服务
                .route("device-service", r -> r
                        .path("/api/v1/devices/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://zncloud-device-service"))
                .build();
    }
}
