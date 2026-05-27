package com.zncloud.billing.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zncloud.billing.model.entity.RateChangeLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RateChangeLogMapper extends BaseMapper<RateChangeLog> {
}
