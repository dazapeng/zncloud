package com.zncloud.user.settlement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zncloud.user.settlement.entity.Withdrawal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WithdrawalMapper extends BaseMapper<Withdrawal> {

    @Select("SELECT * FROM withdrawal WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Withdrawal> findByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM withdrawal WHERE cafe_id = #{cafeId} ORDER BY created_at DESC")
    List<Withdrawal> findByCafeId(@Param("cafeId") String cafeId);
}
