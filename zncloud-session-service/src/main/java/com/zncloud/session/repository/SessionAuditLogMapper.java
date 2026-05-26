package com.zncloud.session.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zncloud.session.model.entity.SessionAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SessionAuditLogMapper extends BaseMapper<SessionAuditLog> {

    @Select("SELECT * FROM session_audit_log WHERE session_id = #{sessionId} ORDER BY created_at DESC")
    List<SessionAuditLog> findBySessionId(@Param("sessionId") String sessionId);
}
