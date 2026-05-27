package com.zncloud.session.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zncloud.session.model.entity.SessionScreenshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SessionScreenshotMapper extends BaseMapper<SessionScreenshot> {

    @Select("SELECT * FROM session_screenshot WHERE session_id = #{sessionId} ORDER BY created_at DESC LIMIT #{limit}")
    List<SessionScreenshot> findLatestBySessionId(@Param("sessionId") String sessionId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM session_screenshot WHERE session_id = #{sessionId}")
    long countBySessionId(@Param("sessionId") String sessionId);
}
