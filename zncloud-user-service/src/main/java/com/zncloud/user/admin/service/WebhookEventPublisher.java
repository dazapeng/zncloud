package com.zncloud.user.admin.service;

import com.zncloud.user.admin.repository.WebhookEventLogRepository;
import com.zncloud.user.admin.entity.WebhookEntity;
import com.zncloud.user.admin.entity.WebhookEventLogEntity;
import com.zncloud.user.admin.enums.DeliveryStatus;
import com.zncloud.user.admin.enums.WebhookStatus;
import com.zncloud.user.admin.repository.WebhookRepository;
import com.zncloud.common.util.crypto.HmacSha256Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Webhook 事件重试调度器
 * 定期扫描待重试的事件并执行投递
 */
@Component
public class WebhookEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(WebhookEventPublisher.class);

    @Autowired
    private WebhookEventLogRepository eventLogRepository;

    @Autowired
    private WebhookRepository webhookRepository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * 每30秒扫描一次待重试的事件
     */
    @Scheduled(fixedDelay = 30000)
    public void retryPendingEvents() {
        List<WebhookEventLogEntity> pendingEvents = eventLogRepository.findPendingEvents(LocalDateTime.now());
        for (WebhookEventLogEntity event : pendingEvents) {
            try {
                WebhookEntity webhook = webhookRepository.selectById(event.getWebhookId());
                if (webhook == null) {
                    eventLogRepository.markFailed(event.getId(), "Webhook not found");
                    continue;
                }
                if (webhook.getStatus() != WebhookStatus.ACTIVE) {
                    eventLogRepository.markFailed(event.getId(), "Webhook disabled");
                    continue;
                }

                String signature = HmacSha256Util.sign(event.getPayload(), webhook.getSecret());
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(webhook.getUrl()))
                        .header("Content-Type", "application/json")
                        .header("X-ZN-Signature", signature)
                        .POST(HttpRequest.BodyPublishers.ofString(event.getPayload()))
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    event.setStatus(DeliveryStatus.SUCCESS);
                    event.setAttemptCount(event.getAttemptCount() + 1);
                    event.setNextRetryAt(null);
                } else {
                    throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
                }
            } catch (Exception e) {
                event.setAttemptCount(event.getAttemptCount() + 1);
                event.setLastError(e.getMessage());
                event.setStatus(DeliveryStatus.FAILED);
                log.warn("Retry failed for eventLogId={}, attempts={}", event.getId(), event.getAttemptCount());
            }
            event.setUpdatedAt(LocalDateTime.now());
            eventLogRepository.updateById(event);
        }
    }
}
