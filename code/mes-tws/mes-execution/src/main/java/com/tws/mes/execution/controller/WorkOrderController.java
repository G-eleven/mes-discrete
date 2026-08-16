package com.tws.mes.execution.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tws.mes.common.result.PageResult;
import com.tws.mes.common.result.Result;
import com.tws.mes.execution.entity.PlanWorkOrder;
import com.tws.mes.execution.service.WorkOrderService;
import com.tws.mes.execution.vo.WorkOrderVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/** 工单管理接口 */
@RestController
@RequestMapping("/api/wo")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService service;

    @GetMapping("/page")
    public Result<PageResult<WorkOrderVO>> page(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String status) {
        return Result.ok(service.page(page, size, keyword, status));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(service.detail(id));
    }

    @Data
    public static class CreateDTO {
        private Long productMaterialId;
        private Long bomId;
        private Long routingId;
        private Integer planQty;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate planStartDate;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate planEndDate;
    }

    /** 创建工单：计划员或管理员 */
    @SaCheckRole(value = {"admin", "planner"}, mode = cn.dev33.satoken.annotation.SaMode.OR)
    @PostMapping("/create")
    public Result<PlanWorkOrder> create(@RequestBody CreateDTO dto) {
        return Result.ok(service.create(dto.getProductMaterialId(), dto.getBomId(), dto.getRoutingId(),
                dto.getPlanQty(), dto.getPlanStartDate(), dto.getPlanEndDate()));
    }

    /** 状态流转：release/start/pause/resume/complete/close */
    @SaCheckRole(value = {"admin", "planner"}, mode = cn.dev33.satoken.annotation.SaMode.OR)
    @PostMapping("/{id}/{action}")
    public Result<Void> transition(@PathVariable Long id, @PathVariable String action) {
        service.transition(id, action);
        return Result.ok();
    }
}
