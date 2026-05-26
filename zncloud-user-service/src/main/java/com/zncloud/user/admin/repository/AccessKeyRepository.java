package com.zncloud.user.admin.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zncloud.user.admin.entity.AccessKeyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.Optional;

@Mapper
public interface AccessKeyRepository extends BaseMapper<AccessKeyEntity> {

    @Select("SELECT * FROM access_keys WHERE key_id = #{keyId}")
    Optional<AccessKeyEntity> findByKeyId(String keyId);
}
