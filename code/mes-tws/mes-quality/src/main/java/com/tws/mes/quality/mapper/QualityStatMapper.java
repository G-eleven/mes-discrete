package com.tws.mes.quality.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 质量统计 SQL（FPY 双口径 / 日趋势 / 不良柏拉图）。
 *
 * 口径（与简历叙事一致）：
 * - 完工数 finished：整机 SN 且 status=DONE
 * - 一次直通 firstPass：完工 SN 中，过站记录从未 NG 且无重测轮次（retest_round 全为 0）
 * - FPY 一次直通率 = firstPass / finished
 * - 最终良率 = finished / (finished + scrapped)（报废不进入交付）
 */
@Mapper
public interface QualityStatMapper {

    /** 按工单汇总 */
    @Select("SELECT w.id AS woId, w.wo_no AS woNo, w.plan_qty AS planQty, " +
            "  COUNT(s.id) AS finished, " +
            "  SUM(CASE WHEN bad.sn IS NULL THEN 1 ELSE 0 END) AS firstPass, " +
            "  (SELECT COUNT(*) FROM sn_registry s2 WHERE s2.work_order_id = w.id AND s2.sn_type='MACHINE' AND s2.status='SCRAP') AS scrapped " +
            "FROM sn_registry s " +
            "JOIN plan_work_order w ON w.id = s.work_order_id " +
            "LEFT JOIN (SELECT DISTINCT sn FROM station_log WHERE record_type='CHECKIN' AND (result='NG' OR retest_round > 0)) bad " +
            "  ON bad.sn = s.sn " +
            "WHERE s.sn_type='MACHINE' AND s.status='DONE' " +
            "GROUP BY w.id, w.wo_no, w.plan_qty ORDER BY w.id")
    List<Map<String, Object>> fpyByWo(@Param("woId") Long woId);

    /** 按日趋势（按 SN 完工时间归集） */
    @Select("SELECT DATE(s.update_time) AS day, COUNT(s.id) AS finished, " +
            "  SUM(CASE WHEN bad.sn IS NULL THEN 1 ELSE 0 END) AS firstPass " +
            "FROM sn_registry s " +
            "LEFT JOIN (SELECT DISTINCT sn FROM station_log WHERE record_type='CHECKIN' AND (result='NG' OR retest_round > 0)) bad " +
            "  ON bad.sn = s.sn " +
            "WHERE s.sn_type='MACHINE' AND s.status='DONE' " +
            "GROUP BY DATE(s.update_time) ORDER BY day")
    List<Map<String, Object>> fpyDaily();

    /** 不良柏拉图（Top N 不良代码） */
    @Select("<script>" +
            "SELECT d.defect_code AS defectCode, IFNULL(c.defect_name, d.defect_code) AS defectName, COUNT(*) AS cnt " +
            "FROM defect_record d LEFT JOIN md_defect_code c ON c.defect_code = d.defect_code " +
            "<where><if test='woId != null'>AND d.work_order_id = #{woId}</if></where>" +
            "GROUP BY d.defect_code, c.defect_name ORDER BY cnt DESC LIMIT 10" +
            "</script>")
    List<Map<String, Object>> defectPareto(@Param("woId") Long woId);
}
