package com.tws.mes.base.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tws.mes.base.entity.MdRouting;
import com.tws.mes.base.entity.MdRoutingOperation;
import com.tws.mes.base.service.MdRoutingService;
import com.tws.mes.common.result.PageResult;
import com.tws.mes.common.result.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 工艺路线：版本化 + 工序明细 + 防呆规则 JSON */
@RestController
@RequestMapping("/api/routing")
@RequiredArgsConstructor
public class MdRoutingController {

    private final MdRoutingService service;

    @GetMapping("/page")
    public Result<PageResult<MdRouting>> page(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword) {
        return Result.ok(service.page(page, size, keyword));
    }

    @GetMapping("/released")
    public Result<List<MdRouting>> released(@RequestParam(required = false) Long productMaterialId) {
        return Result.ok(service.releasedList(productMaterialId));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(service.detail(id));
    }

    @Data
    public static class SaveDTO {
        private MdRouting routing;
        private List<MdRoutingOperation> operations;
    }

    @SaCheckRole("admin")
    @PostMapping("/save")
    public Result<Void> save(@RequestBody SaveDTO dto) {
        service.save(dto.getRouting(), dto.getOperations());
        return Result.ok();
    }

    @SaCheckRole("admin")
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        service.publish(id);
        return Result.ok();
    }

    @SaCheckRole("admin")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
