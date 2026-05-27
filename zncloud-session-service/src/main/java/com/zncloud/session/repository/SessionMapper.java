package com.zncloud.session.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zncloud.session.model.entity.Session;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SessionMapper extends BaseMapper<Session> {
}
