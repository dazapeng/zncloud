package com.zncloud.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * AK/SK 认证客户端
 * 通过 HTTP 调用 user-service 验证签名
 */
@Component
public class AkSkAuthClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public AkSkAuthClient(@Value("${aks.auth.service-url:http://zncloud-user-service:8081}") String serviceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(serviceUrl)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 调用 user-service 验证 AK/SK 签名
     */
    public Mono<Boolean> verifySignature(String keyId, String signature, String stringToSign) {
        return webClient.post()
                .uri("/api/v1/admin/access-keys/verify")
                .header("Content-Type", "application/json")
                .bodyValue(new VerifyRequest(keyId, signature, stringToSign))
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseResponse)
                .onErrorReturn(false);
    }

    private boolean parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.get("data");
            if (data != null && data.has("valid")) {
                return data.get("valid").asBoolean();
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static class VerifyRequest {
        public String keyId;
        public String signature;
        public String stringToSign;

        VerifyRequest(String keyId, String signature, String stringToSign) {
            this.keyId = keyId;
            this.signature = signature;
            this.stringToSign = stringToSign;
        }
    }
}
