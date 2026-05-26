package com.zncloud.user.settlement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zncloud.user.settlement.entity.CafeSettlement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CafeSettlementMapper extends BaseMapper<CafeSettlement> {

    @Select("SELECT COALESCE(SUM(total_revenue), 0) FROM cafe_settlement WHERE cafe_id = #{cafeId} AND period_start = #{date} AND period_end = #{date}")
    java.math.BigDecimal sumTodayRevenue(@Param("cafeId") String cafeId, @Param("date") LocalDate date);

    @Select("SELECT COALESCE(SUM(cafe_share), 0) FROM cafe_settlement WHERE cafe_id = #{cafeId} AND period_start = #{date} AND period_end = #{date}")
    java.math.BigDecimal sumTodayCafeShare(@Param("cafeId") String cafeId, @Param("date") LocalDate date);

    @Select("SELECT COALESCE(SUM(total_online_hours), 0) FROM cafe_settlement WHERE cafe_id = #{cafeId} AND period_start = #{date} AND period_end = #{date}")
    java.math.BigDecimal sumTodayOnlineHours(@Param("cafeId") String cafeId, @Param("date") LocalDate date);

    @Select("SELECT COALESCE(SUM(total_sessions), 0) FROM cafe_settlement WHERE cafe_id = #{cafeId} AND period_start = #{date} AND period_end = #{date}")
    Integer sumTodaySessions(@Param("cafeId") String cafeId, @Param("date") LocalDate date);

    @Select("SELECT COALESCE(SUM(total_online_hours), 0) FROM cafe_settlement WHERE cafe_id = #{cafeId} AND period_start >= #{start} AND period_end <= #{end}")
    java.math.BigDecimal sumPeriodOnlineHours(@Param("cafeId") String cafeId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT COALESCE(SUM(total_revenue), 0) FROM cafe_settlement WHERE cafe_id = #{cafeId} AND period_start >= #{start} AND period_end <= #{end}")
    java.math.BigDecimal sumPeriodRevenue(@Param("cafeId") String cafeId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT COALESCE(SUM(cafe_share), 0) FROM cafe_settlement WHERE cafe_id = #{cafeId} AND period_start >= #{start} AND period_end <= #{end}")
    java.math.BigDecimal sumPeriodCafeShare(@Param("cafeId") String cafeId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT COALESCE(SUM(total_sessions), 0) FROM cafe_settlement WHERE cafe_id = #{cafeId} AND period_start >= #{start} AND period_end <= #{end}")
    Integer sumPeriodSessions(@Param("cafeId") String cafeId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT COALESCE(SUM(cafe_share), 0) FROM cafe_settlement WHERE cafe_id = #{cafeId} AND status IN ('CONFIRMED', 'SETTLED')")
    java.math.BigDecimal sumTotalCafeShare(@Param("cafeId") String cafeId);

    @Select("SELECT COALESCE(SUM(total_revenue), 0) FROM cafe_settlement WHERE cafe_id = #{cafeId} AND status IN ('CONFIRMED', 'SETTLED')")
    java.math.BigDecimal sumTotalRevenue(@Param("cafeId") String cafeId);

    @Select("SELECT COALESCE(SUM(total_sessions), 0) FROM cafe_settlement WHERE cafe_id = #{cafeId} AND status IN ('CONFIRMED', 'SETTLED')")
    Integer sumTotalSessions(@Param("cafeId") String cafeId);

    @Select("SELECT COALESCE(SUM(total_online_hours), 0) FROM cafe_settlement WHERE cafe_id = #{cafeId} AND status IN ('CONFIRMED', 'SETTLED')")
    java.math.BigDecimal sumTotalOnlineHours(@Param("cafeId") String cafeId);

    @Select("SELECT COALESCE(SUM(cafe_share), 0) FROM cafe_settlement WHERE cafe_id = #{cafeId} AND status = 'PENDING'")
    java.math.BigDecimal sumPendingSettlement(@Param("cafeId") String cafeId);

    @Select("SELECT * FROM cafe_settlement WHERE cafe_id = #{cafeId} AND period_type = #{periodType} ORDER BY period_start DESC")
    List<CafeSettlement> findByCafeIdAndPeriodType(@Param("cafeId") String cafeId, @Param("periodType") String periodType);

    @Select("SELECT * FROM cafe_settlement WHERE cafe_id = #{cafeId} ORDER BY period_start DESC")
    List<CafeSettlement> findByCafeId(@Param("cafeId") String cafeId);
}
