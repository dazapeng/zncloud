package com.zncloud.user.admin.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zncloud.user.admin.entity.WebhookEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WebhookRepository extends BaseMapper<WebhookEntity> {

    /**
     * 查询订阅了指定事件类型的活跃 Webhook
     * events 字段存储 JSON 数组字符串，如 ["session.started","session.ended"]
     */
    @Select("SELECT * FROM webhooks WHERE status = 'ACTIVE' AND events LIKE CONCAT('%', #{eventType}, '%')")
    List<WebhookEntity> findActiveByEventType(String eventType);
}
