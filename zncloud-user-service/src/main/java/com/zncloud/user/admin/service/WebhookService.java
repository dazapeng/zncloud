package com.zncloud.user.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zncloud.common.util.crypto.HmacSha256Util;
import com.zncloud.user.admin.dto.CreateWebhookRequest;
import com.zncloud.user.admin.dto.UpdateWebhookRequest;
import com.zncloud.user.admin.dto.WebhookResponse;
import com.zncloud.user.admin.entity.WebhookEntity;
import com.zncloud.user.admin.entity.WebhookEventLogEntity;
import com.zncloud.user.admin.enums.DeliveryStatus;
import com.zncloud.user.admin.enums.WebhookEventType;
import com.zncloud.user.admin.enums.WebhookStatus;
import com.zncloud.user.admin.repository.WebhookRepository;
import com.zncloud.user.admin.repository.WebhookEventLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private static final List<Long> RETRY_DELAYS_SECONDS = List.of(5L, 30L, 300L); // 5s, 30s, 5min

    @Autowired
    private WebhookRepository webhookRepository;

    @Autowired
    private WebhookEventLogRepository eventLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * 创建 Webhook 订阅
     */
    @Transactional
    public WebhookResponse createWebhook(CreateWebhookRequest request, Long createdBy) {
        WebhookEntity entity = new WebhookEntity();
        entity.setName(request.getName());
        entity.setUrl(request.getUrl());
        try {
            entity.setEvents(objectMapper.writeValueAsString(request.getEvents()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("事件序列化失败", e);
        }
        entity.setSecret(request.getSecret() != null ? request.getSecret() : HmacSha256Util.generateWebhookSecret());
        entity.setStatus(WebhookStatus.ACTIVE);
        entity.setRetryCount(3);
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        webhookRepository.insert(entity);
        return WebhookResponse.fromEntity(entity);
    }

    /**
     * 查询 Webhook 列表
     */
    public List<WebhookResponse> listWebhooks() {
        return webhookRepository.selectList(null)
                .stream()
                .map(WebhookResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 更新 Webhook 配置
     */
    @Transactional
    public WebhookResponse updateWebhook(Long id, UpdateWebhookRequest request) {
        WebhookEntity entity = webhookRepository.selectById(id);
        if (entity == null) {
            throw new RuntimeException("Webhook 不存在");
        }
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getUrl() != null) entity.setUrl(request.getUrl());
        if (request.getEvents() != null) {
            try {
                entity.setEvents(objectMapper.writeValueAsString(request.getEvents()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("事件序列化失败", e);
            }
        }
        if (request.getSecret() != null) entity.setSecret(request.getSecret());
        entity.setUpdatedAt(LocalDateTime.now());
        webhookRepository.updateById(entity);
        return WebhookResponse.fromEntity(entity);
    }

    /**
     * 删除 Webhook 订阅
     */
    @Transactional
    public void deleteWebhook(Long id) {
        WebhookEntity entity = webhookRepository.selectById(id);
        if (entity == null) {
            throw new RuntimeException("Webhook 不存在");
        }
        webhookRepository.deleteById(id);
    }

    /**
     * 发布事件到所有订阅的 Webhook
     */
    @Async
    public void publishEvent(String eventType, Map<String, Object> data) {
        List<WebhookEntity> webhooks = webhookRepository.findActiveByEventType(eventType);
        if (webhooks.isEmpty()) {
            log.debug("No webhook subscriptions for event: {}", eventType);
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event_type", eventType);
        payload.put("timestamp", LocalDateTime.now().toString());
        payload.put("data", data);

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize webhook payload", e);
            return;
        }

        for (WebhookEntity webhook : webhooks) {
            deliverEventAsync(webhook, eventType, payloadJson);
        }
    }

    /**
     * 异步投递事件到单个 Webhook
     */
    private void deliverEventAsync(WebhookEntity webhook, String eventType, String payloadJson) {
        WebhookEventLogEntity logEntity = new WebhookEventLogEntity();
        logEntity.setWebhookId(webhook.getId());
        logEntity.setEventType(eventType);
        logEntity.setPayload(payloadJson);
        logEntity.setStatus(DeliveryStatus.PENDING);
        logEntity.setAttemptCount(0);
        logEntity.setCreatedAt(LocalDateTime.now());
        logEntity.setUpdatedAt(LocalDateTime.now());
        eventLogRepository.insert(logEntity);

        try {
            doDelivery(webhook, payloadJson, logEntity);
        } catch (Exception e) {
            log.error("Webhook delivery failed: webhookId={}, error={}", webhook.getId(), e.getMessage());
            handleDeliveryFailure(webhook, logEntity, e.getMessage());
        }
    }

    /**
     * 执行 HTTP POST 投递
     */
    private void doDelivery(WebhookEntity webhook, String payloadJson, WebhookEventLogEntity logEntity) throws Exception {
        String signature = HmacSha256Util.sign(payloadJson, webhook.getSecret());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhook.getUrl()))
                .header("Content-Type", "application/json")
                .header("X-ZN-Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            logEntity.setStatus(DeliveryStatus.SUCCESS);
            logEntity.setAttemptCount(logEntity.getAttemptCount() + 1);
        } else {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    /**
     * 处理投递失败（重试逻辑）
     */
    private void handleDeliveryFailure(WebhookEntity webhook, WebhookEventLogEntity logEntity, String error) {
        logEntity.setAttemptCount(logEntity.getAttemptCount() + 1);
        logEntity.setLastError(error);

        if (logEntity.getAttemptCount() >= webhook.getRetryCount()) {
            logEntity.setStatus(DeliveryStatus.FAILED);
            log.warn("Webhook delivery exhausted all retries: webhookId={}, attempts={}",
                    webhook.getId(), logEntity.getAttemptCount());
        } else {
            // 计算下次重试时间
            int retryIndex = logEntity.getAttemptCount() - 1;
            if (retryIndex >= RETRY_DELAYS_SECONDS.size()) {
                retryIndex = RETRY_DELAYS_SECONDS.size() - 1;
            }
            long delaySeconds = RETRY_DELAYS_SECONDS.get(retryIndex);
            logEntity.setStatus(DeliveryStatus.PENDING);
            logEntity.setNextRetryAt(LocalDateTime.now().plusSeconds(delaySeconds));
            log.info("Webhook delivery scheduled for retry: webhookId={}, attempt={}/{}, delay={}s",
                    webhook.getId(), logEntity.getAttemptCount(), webhook.getRetryCount(), delaySeconds);
        }
        logEntity.setUpdatedAt(LocalDateTime.now());
        eventLogRepository.updateById(logEntity);
    }
}
