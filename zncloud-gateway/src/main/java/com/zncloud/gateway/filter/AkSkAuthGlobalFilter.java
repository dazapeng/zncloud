package com.zncloud.gateway.filter;

import com.zncloud.common.util.crypto.HmacSha256Util;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * AK/SK 认证全局过滤器
 * 验证外部合作伙伴通过 AK/SK 方式的 API 访问
 * 优先级高于 JWT 认证
 *
 * 认证方式:
 *   Authorization: ZN <key_id>:<signature>
 *   签名算法: HMAC-SHA256(<key_id> + "\n" + <timestamp> + "\n" + <http_method> + "\n" + <path> + "\n" + <query_string>)
 *   需同时提供 X-ZN-Timestamp 请求头（UTC 时间戳，5分钟内有效）
 */
@Component
@ConfigurationProperties(prefix = "aks.auth")
@Setter
public class AkSkAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher pathMatcher = new AntPathMatcher();
    private static final long SIGNATURE_VALIDITY_SECONDS = 300;

    private List<String> skipPaths;
    private final AkSkAuthClient akSkAuthClient;

    public AkSkAuthGlobalFilter(AkSkAuthClient akSkAuthClient) {
        this.akSkAuthClient = akSkAuthClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isSkipPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("ZN ")) {
            return chain.filter(exchange);
        }

        String authValue = authHeader.substring(3).trim();
        String[] parts = authValue.split(":", 2);
        if (parts.length != 2) {
            return unauthorized(exchange.getResponse(), "AK/SK 认证格式错误，应为: ZN <key_id>:<signature>");
        }

        String keyId = parts[0];
        String signature = parts[1];

        String timestampStr = request.getHeaders().getFirst("X-ZN-Timestamp");
        if (timestampStr == null) {
            return unauthorized(exchange.getResponse(), "缺少 X-ZN-Timestamp 请求头");
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            return unauthorized(exchange.getResponse(), "X-ZN-Timestamp 格式错误，应为 Unix 时间戳（秒）");
        }

        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - timestamp) > SIGNATURE_VALIDITY_SECONDS) {
            return unauthorized(exchange.getResponse(), "请求已过期，请检查系统时间或重新生成签名");
        }

        String httpMethod = request.getMethod().name();
        String queryString = request.getURI().getRawQuery() != null ? request.getURI().getRawQuery() : "";
        String stringToSign = keyId + "\n" + timestamp + "\n" + httpMethod + "\n" + path + "\n" + queryString;

        return akSkAuthClient.verifySignature(keyId, signature, stringToSign)
                .flatMap(isValid -> {
                    if (!Boolean.TRUE.equals(isValid)) {
                        return unauthorized(exchange.getResponse(), "AK/SK 认证失败：密钥无效或签名错误");
                    }
                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header("X-Auth-Type", "AK_SK")
                            .header("X-Access-Key-Id", keyId)
                            .build();
                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                });
    }

    @Override
    public int getOrder() {
        return -110;
    }

    private boolean isSkipPath(String path) {
        if (skipPaths == null || skipPaths.isEmpty()) return false;
        return skipPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
