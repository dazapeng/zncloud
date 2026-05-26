package com.zncloud.user.admin.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zncloud.user.admin.entity.WebhookEventLogEntity;
import com.zncloud.user.admin.enums.DeliveryStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WebhookEventLogRepository extends BaseMapper<WebhookEventLogEntity> {

    @Select("SELECT * FROM webhook_event_logs WHERE status = 'PENDING' AND next_retry_at <= #{now} ORDER BY next_retry_at ASC LIMIT 100")
    List<WebhookEventLogEntity> findPendingEvents(LocalDateTime now);

    @Update("UPDATE webhook_event_logs SET status = 'FAILED', last_error = #{error}, updated_at = NOW() WHERE id = #{id}")
    void markFailed(Long id, String error);
}
