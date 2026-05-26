package com.zncloud.billing.repository;

import com.zncloud.billing.model.entity.AuditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {

    /**
     * 复合查询: 按用户ID、操作类型和时间范围筛选审计日志
     */
    @Select("SELECT * FROM audit_log " +
            "WHERE user_id = #{userId} " +
            "AND action = #{action} " +
            "AND created_at BETWEEN #{startTime} AND #{endTime} " +
            "ORDER BY created_at DESC")
    List<AuditLog> findByUserIdAndActionAndCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("action") String action,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
