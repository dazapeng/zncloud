package com.zncloud.session.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zncloud.session.model.entity.ContentCheckLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContentCheckLogMapper extends BaseMapper<ContentCheckLog> {

    @Select("SELECT * FROM content_check_log WHERE session_id = #{sessionId} ORDER BY checked_at DESC")
    List<ContentCheckLog> findBySessionId(@Param("sessionId") String sessionId);

    @Select("SELECT * FROM content_check_log WHERE risk_level IN ('HIGH', 'CRITICAL') ORDER BY checked_at DESC")
    List<ContentCheckLog> findViolations();
}
