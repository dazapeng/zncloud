package com.zncloud.billing.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 费率变更事件监听器
 * 订阅 Redis Pub/Sub 频道，可在此扩展 WebSocket 推送
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateChangeEventListener implements MessageListener {

    private final RedisMessageListenerContainer listenerContainer;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        listenerContainer.addMessageListener(this, new ChannelTopic(RateChangeEventPublisher.RATE_CHANGE_CHANNEL));
        log.info("费率变更事件监听器已注册: channel={}", RateChangeEventPublisher.RATE_CHANGE_CHANNEL);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            RateChangeEvent event = objectMapper.readValue(body, RateChangeEvent.class);
            log.info("收到费率变更事件: eventType={}, cafeId={}, configLevel={}, newPrice={}",
                    event.getEventType(), event.getCafeId(), event.getConfigLevel(), event.getNewPrice());

            // TODO: 扩展 WebSocket 推送，供前端实时同步
            // websocketService.broadcast("/topic/rate/change", event);
        } catch (Exception e) {
            log.error("费率变更事件处理失败", e);
        }
    }
}
