package com.tws.mes.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tws.mes.execution.entity.PlanWorkOrder;
import com.tws.mes.execution.vo.WorkOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PlanWorkOrderMapper extends BaseMapper<PlanWorkOrder> {

    /**
     * 工单分页（关联物料名/路线名）。
     * 用注解 SQL 而不是内存装配：分页在数据库完成，数据量大也不会 N+1。
     */
    @Select("<script>" +
            "SELECT w.*, m.material_name AS productMaterialName, m.material_code AS productMaterialCode, " +
            "       r.routing_name AS routingName " +
            "FROM plan_work_order w " +
            "LEFT JOIN md_material m ON m.id = w.product_material_id " +
            "LEFT JOIN md_routing r ON r.id = w.routing_id " +
            "<where>" +
            "    <if test='keyword != null and keyword != \"\"'>AND w.wo_no LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "    <if test='status != null and status != \"\"'>AND w.status = #{status}</if>" +
            "</where>" +
            "ORDER BY w.id DESC" +
            "</script>")
    IPage<WorkOrderVO> pageWo(Page<WorkOrderVO> page, @Param("keyword") String keyword, @Param("status") String status);
}
