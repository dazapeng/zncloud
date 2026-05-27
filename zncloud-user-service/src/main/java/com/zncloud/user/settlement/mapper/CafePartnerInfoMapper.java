package com.zncloud.user.settlement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zncloud.user.settlement.entity.CafePartnerInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface CafePartnerInfoMapper extends BaseMapper<CafePartnerInfo> {

    @Select("SELECT * FROM cafe_partner_info WHERE cafe_id = #{cafeId} AND status = 'ACTIVE' LIMIT 1")
    Optional<CafePartnerInfo> findByCafeId(@Param("cafeId") String cafeId);

    @Select("SELECT * FROM cafe_partner_info WHERE user_id = #{userId} AND status = 'ACTIVE' LIMIT 1")
    Optional<CafePartnerInfo> findByUserId(@Param("userId") Long userId);
}
