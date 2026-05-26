package com.zncloud.device.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zncloud.device.model.dto.PendingCommand;
import org.apache.ibatis.annotations.Mapper;

/**
 * 待处理电源命令 Mapper
 */
@Mapper
public interface PendingCommandMapper extends BaseMapper<PendingCommand> {
}
