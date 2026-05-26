package com.zncloud.device.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zncloud.device.model.Device;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import org.apache.ibatis.annotations.Insert;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OperationMapper {

    /**
     * 区域统计查询（按省份/城市/区县分组）
     */
    @Select("<script>" +
            "SELECT d.province, d.city, d.district, " +
            "       COUNT(*) as device_count, " +
            "       SUM(CASE WHEN d.status = 'ONLINE' OR d.status = 'IN_USE' THEN 1 ELSE 0 END) as online_count, " +
            "       SUM(CASE WHEN d.status = 'IN_USE' THEN 1 ELSE 0 END) as in_use_count, " +
            "       SUM(CASE WHEN d.status = 'OFFLINE' THEN 1 ELSE 0 END) as offline_count, " +
            "       ROUND(SUM(CASE WHEN d.status = 'ONLINE' OR d.status = 'IN_USE' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as online_rate, " +
            "       ROUND(SUM(CASE WHEN d.status = 'IN_USE' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as utilization_rate, " +
            "       ROUND(AVG(d.price_per_hour), 2) as avg_price, " +
            "       COALESCE(SUM(d.total_earnings), 0) as total_earnings " +
            "FROM device d " +
            "LEFT JOIN cafe c ON d.cafe_id = c.id " +
            "WHERE d.is_deleted = 0 " +
            "  <if test='province != null and province != \"\"'> AND d.province = #{province}</if> " +
            "  <if test='city != null and city != \"\"'> AND d.city = #{city}</if> " +
            "  <if test='isp != null and isp != \"\"'> AND d.isp = #{isp}</if> " +
            "  <if test='configLevel != null and configLevel != \"\"'> AND d.config_level = #{configLevel}</if> " +
            "  <if test='minPrice != null'> AND d.price_per_hour &gt;= #{minPrice}</if> " +
            "  <if test='maxPrice != null'> AND d.price_per_hour &lt;= #{maxPrice}</if> " +
            "GROUP BY d.province, d.city, d.district " +
            "ORDER BY d.province, d.city" +
            "</script>")
    List<Map<String, Object>> selectRegionStats(
            @Param("province") String province,
            @Param("city") String city,
            @Param("isp") String isp,
            @Param("configLevel") String configLevel,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice);

    /**
     * ISP统计查询（按运营商分组）
     */
    @Select("<script>" +
            "SELECT d.isp, " +
            "       COUNT(*) as device_count, " +
            "       SUM(CASE WHEN d.status = 'ONLINE' OR d.status = 'IN_USE' THEN 1 ELSE 0 END) as online_count, " +
            "       ROUND(AVG(d.price_per_hour), 2) as avg_price " +
            "FROM device d " +
            "LEFT JOIN cafe c ON d.cafe_id = c.id " +
            "WHERE d.is_deleted = 0 " +
            "  <if test='province != null and province != \"\"'> AND d.province = #{province}</if> " +
            "  <if test='city != null and city != \"\"'> AND d.city = #{city}</if> " +
            "  <if test='isp != null and isp != \"\"'> AND d.isp = #{isp}</if> " +
            "  <if test='configLevel != null and configLevel != \"\"'> AND d.config_level = #{configLevel}</if> " +
            "  <if test='minPrice != null'> AND d.price_per_hour &gt;= #{minPrice}</if> " +
            "  <if test='maxPrice != null'> AND d.price_per_hour &lt;= #{maxPrice}</if> " +
            "GROUP BY d.isp " +
            "ORDER BY device_count DESC" +
            "</script>")
    List<Map<String, Object>> selectIspStats(
            @Param("province") String province,
            @Param("city") String city,
            @Param("isp") String isp,
            @Param("configLevel") String configLevel,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice);

    /**
     * 配置等级统计查询（按配置等级分组）
     */
    @Select("<script>" +
            "SELECT d.config_level as config_level, " +
            "       COUNT(*) as device_count, " +
            "       SUM(CASE WHEN d.status = 'ONLINE' OR d.status = 'IN_USE' THEN 1 ELSE 0 END) as online_count, " +
            "       SUM(CASE WHEN d.status = 'IN_USE' THEN 1 ELSE 0 END) as in_use_count, " +
            "       ROUND(AVG(d.price_per_hour), 2) as avg_price " +
            "FROM device d " +
            "LEFT JOIN cafe c ON d.cafe_id = c.id " +
            "WHERE d.is_deleted = 0 " +
            "  <if test='province != null and province != \"\"'> AND d.province = #{province}</if> " +
            "  <if test='city != null and city != \"\"'> AND d.city = #{city}</if> " +
            "  <if test='isp != null and isp != \"\"'> AND d.isp = #{isp}</if> " +
            "  <if test='configLevel != null and configLevel != \"\"'> AND d.config_level = #{configLevel}</if> " +
            "  <if test='minPrice != null'> AND d.price_per_hour &gt;= #{minPrice}</if> " +
            "  <if test='maxPrice != null'> AND d.price_per_hour &lt;= #{maxPrice}</if> " +
            "GROUP BY d.config_level " +
            "ORDER BY device_count DESC" +
            "</script>")
    List<Map<String, Object>> selectConfigStats(
            @Param("province") String province,
            @Param("city") String city,
            @Param("isp") String isp,
            @Param("configLevel") String configLevel,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice);

    /**
     * 价格区间统计查询
     */
    @Select("<script>" +
            "SELECT " +
            "  CASE " +
            "    WHEN d.price_per_hour &lt; 1 THEN '0-1元' " +
            "    WHEN d.price_per_hour &lt; 2 THEN '1-2元' " +
            "    WHEN d.price_per_hour &lt; 3 THEN '2-3元' " +
            "    ELSE '3+元' " +
            "  END as range_name, " +
            "  COUNT(*) as device_count, " +
            "  SUM(CASE WHEN d.status = 'ONLINE' OR d.status = 'IN_USE' THEN 1 ELSE 0 END) as online_count, " +
            "  ROUND(AVG(d.price_per_hour), 2) as avg_price " +
            "FROM device d " +
            "LEFT JOIN cafe c ON d.cafe_id = c.id " +
            "WHERE d.is_deleted = 0 " +
            "  <if test='province != null and province != \"\"'> AND d.province = #{province}</if> " +
            "  <if test='city != null and city != \"\"'> AND d.city = #{city}</if> " +
            "  <if test='isp != null and isp != \"\"'> AND d.isp = #{isp}</if> " +
            "  <if test='configLevel != null and configLevel != \"\"'> AND d.config_level = #{configLevel}</if> " +
            "  <if test='minPrice != null'> AND d.price_per_hour &gt;= #{minPrice}</if> " +
            "  <if test='maxPrice != null'> AND d.price_per_hour &lt;= #{maxPrice}</if> " +
            "GROUP BY range_name " +
            "ORDER BY MIN(d.price_per_hour)" +
            "</script>")
    List<Map<String, Object>> selectPriceRangeStats(
            @Param("province") String province,
            @Param("city") String city,
            @Param("isp") String isp,
            @Param("configLevel") String configLevel,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice);

    /**
     * 运营视图设备列表（分页）
     */
    @Select("<script>" +
            "SELECT d.id, d.cafe_id, d.cafe_name, d.province, d.city, d.district, " +
            "       d.isp, d.config_level, d.price_per_hour, d.status, " +
            "       d.last_online_at, d.total_online_hours, d.total_earnings, " +
            "       d.cpu_info, d.gpu_info, d.memory_gb " +
            "FROM device d " +
            "LEFT JOIN cafe c ON d.cafe_id = c.id " +
            "WHERE d.is_deleted = 0 " +
            "  <if test='province != null and province != \"\"'> AND d.province = #{province}</if> " +
            "  <if test='city != null and city != \"\"'> AND d.city = #{city}</if> " +
            "  <if test='isp != null and isp != \"\"'> AND d.isp = #{isp}</if> " +
            "  <if test='configLevel != null and configLevel != \"\"'> AND d.config_level = #{configLevel}</if> " +
            "  <if test='minPrice != null'> AND d.price_per_hour &gt;= #{minPrice}</if> " +
            "  <if test='maxPrice != null'> AND d.price_per_hour &lt;= #{maxPrice}</if> " +
            "ORDER BY d.create_time DESC" +
            "</script>")
    IPage<Device> selectOperatorDevices(
            Page<?> page,
            @Param("province") String province,
            @Param("city") String city,
            @Param("isp") String isp,
            @Param("configLevel") String configLevel,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice);

    /**
     * 区域汇总统计（省份/城市层级）
     */
    @Select("<script>" +
            "SELECT d.province, d.city, " +
            "       COUNT(*) as device_count, " +
            "       SUM(CASE WHEN d.status = 'ONLINE' OR d.status = 'IN_USE' THEN 1 ELSE 0 END) as online_count, " +
            "       SUM(CASE WHEN d.status = 'IN_USE' THEN 1 ELSE 0 END) as in_use_count, " +
            "       ROUND(SUM(CASE WHEN d.status = 'ONLINE' OR d.status = 'IN_USE' THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as online_rate, " +
            "       ROUND(AVG(d.price_per_hour), 2) as avg_price, " +
            "       COALESCE(SUM(d.total_earnings), 0) as total_earnings " +
            "FROM device d " +
            "LEFT JOIN cafe c ON d.cafe_id = c.id " +
            "WHERE d.is_deleted = 0 " +
            "  <if test='province != null and province != \"\"'> AND d.province = #{province}</if> " +
            "  <if test='city != null and city != \"\"'> AND d.city = #{city}</if> " +
            "  <if test='isp != null and isp != \"\"'> AND d.isp = #{isp}</if> " +
            "  <if test='configLevel != null and configLevel != \"\"'> AND d.config_level = #{configLevel}</if> " +
            "  <if test='minPrice != null'> AND d.price_per_hour &gt;= #{minPrice}</if> " +
            "  <if test='maxPrice != null'> AND d.price_per_hour &lt;= #{maxPrice}</if> " +
            "GROUP BY d.province, d.city " +
            "ORDER BY d.province, d.city" +
            "</script>")
    List<Map<String, Object>> selectRegionSummary(
            @Param("province") String province,
            @Param("city") String city,
            @Param("isp") String isp,
            @Param("configLevel") String configLevel,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice);

    // ==================== Filter / Distinct Queries ====================

    @Select("SELECT DISTINCT province FROM device WHERE is_deleted = 0 AND province IS NOT NULL ORDER BY province")
    List<Map<String, Object>> selectDistinctProvinces();

    @Select("SELECT DISTINCT city FROM device WHERE is_deleted = 0 AND city IS NOT NULL ORDER BY city")
    List<Map<String, Object>> selectDistinctCities();

    @Select("SELECT DISTINCT isp FROM device WHERE is_deleted = 0 AND isp IS NOT NULL ORDER BY isp")
    List<Map<String, Object>> selectDistinctIsps();

    // ==================== Insert Operations ====================

    @Insert("INSERT INTO batch_price_change_log (action_type, filter_criteria, adjustment_type, adjustment_value, " +
            "affected_count, operator_id, created_at) VALUES (#{actionType}, #{filterCriteria}, #{adjustmentType}, " +
            "#{adjustmentValue}, #{affectedCount}, #{operatorId}, #{createdAt})")
    void insertBatchPriceChangeLog(
            @Param("actionType") String actionType,
            @Param("filterCriteria") String filterCriteria,
            @Param("adjustmentType") String adjustmentType,
            @Param("adjustmentValue") BigDecimal adjustmentValue,
            @Param("affectedCount") Integer affectedCount,
            @Param("operatorId") String operatorId,
            @Param("createdAt") LocalDateTime createdAt);

    @Insert("INSERT INTO operation_notification (title, content, type, target_type, target_value, " +
            "publisher_id, created_at) VALUES (#{title}, #{content}, #{type}, #{targetType}, " +
            "#{targetValue}, #{publisherId}, #{createdAt})")
    void insertNotification(
            @Param("title") String title,
            @Param("content") String content,
            @Param("type") String type,
            @Param("targetType") String targetType,
            @Param("targetValue") String targetValue,
            @Param("publisherId") String publisherId,
            @Param("createdAt") LocalDateTime createdAt);

    // ==================== List Queries ====================

    @Select("<script>" +
            "SELECT * FROM operation_notification WHERE is_deleted = 0 " +
            "  <if test='type != null and type != \"\"'> AND type = #{type}</if> " +
            "ORDER BY created_at DESC" +
            "</script>")
    IPage<Map<String, Object>> selectNotifications(
            Page<?> page,
            @Param("type") String type);

    @Select("SELECT * FROM operation_notification WHERE is_deleted = 0 ORDER BY created_at DESC")
    List<Map<String, Object>> selectAllNotifications();
}
