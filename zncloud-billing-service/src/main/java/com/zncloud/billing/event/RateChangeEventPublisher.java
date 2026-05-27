package com.zncloud.billing.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 费率变更事件发布器
 * 通过 Redis Pub/Sub 推送费率变更通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateChangeEventPublisher {

    /** Redis 频道名称 */
    public static final String RATE_CHANGE_CHANNEL = "zncloud:rate:change";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 发布费率变更事件
     */
    public void publish(RateChangeEvent event) {
        try {
            event.setEventId(UUID.randomUUID().toString().replace("-", ""));
            event.setTimestamp(LocalDateTime.now());

            String message = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(RATE_CHANGE_CHANNEL, message);

            log.info("费率变更事件已发布: channel={}, eventType={}, cafeId={}, configLevel={}",
                    RATE_CHANGE_CHANNEL, event.getEventType(), event.getCafeId(), event.getConfigLevel());
        } catch (Exception e) {
            log.error("费率变更事件发布失败", e);
        }
    }
}
