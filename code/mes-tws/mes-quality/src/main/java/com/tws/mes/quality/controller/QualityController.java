package com.tws.mes.quality.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tws.mes.common.result.PageResult;
import com.tws.mes.common.result.Result;
import com.tws.mes.execution.entity.RepairRecord;
import com.tws.mes.quality.entity.FaiRecord;
import com.tws.mes.quality.mapper.FaiRecordMapper;
import com.tws.mes.quality.service.FpyService;
import com.tws.mes.quality.service.QualityService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 质量管理：不良单 / 维修 / 首件 / FPY 双口径统计 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QualityController {

    private final QualityService qualityService;
    private final FpyService fpyService;
    private final FaiRecordMapper faiMapper;

    /* ---------- 不良单 ---------- */

    @GetMapping("/defect/page")
    public Result<PageResult<Map<String, Object>>> defectPage(@RequestParam(defaultValue = "1") long page,
                                                              @RequestParam(defaultValue = "10") long size,
                                                              @RequestParam(required = false) String sn,
                                                              @RequestParam(required = false) Long workOrderId,
                                                              @RequestParam(required = false) String status,
                                                              @RequestParam(required = false) String defectCode) {
        return Result.ok(qualityService.defectPage(page, size, sn, workOrderId, status, defectCode));
    }

    @Data
    public static class RepairDTO extends QualityService.RepairDTO {}

    /** 维修登记（quality/admin） */
    @SaCheckRole(value = {"admin", "quality"}, mode = SaMode.OR)
    @PostMapping("/defect/repair")
    public Result<Void> repair(@RequestBody RepairDTO dto) {
        qualityService.repair(dto);
        return Result.ok();
    }

    @Data
    public static class ManualDTO {
        private String sn;
        private String defectCode;
        private String desc;
        private String discoverType;
    }

    /** 复检/抽检手动开单 */
    @SaCheckRole(value = {"admin", "quality"}, mode = SaMode.OR)
    @PostMapping("/defect/manual")
    public Result<Void> manual(@RequestBody ManualDTO dto) {
        qualityService.manualDefect(dto.getSn(), dto.getDefectCode(), dto.getDesc(), dto.getDiscoverType());
        return Result.ok();
    }

    @GetMapping("/defect/repairs")
    public Result<List<RepairRecord>> repairs(@RequestParam String sn) {
        return Result.ok(qualityService.repairsOf(sn));
    }

    /* ---------- FPY 双口径 ---------- */

    @GetMapping("/fpy/summary")
    public Result<Map<String, Object>> fpySummary(@RequestParam(required = false) Long woId) {
        return Result.ok(fpyService.summary(woId));
    }

    @GetMapping("/fpy/daily")
    public Result<List<Map<String, Object>>> fpyDaily() {
        return Result.ok(fpyService.daily());
    }

    @GetMapping("/fpy/pareto")
    public Result<List<Map<String, Object>>> pareto(@RequestParam(required = false) Long woId) {
        return Result.ok(fpyService.pareto(woId));
    }

    /* ---------- 首件检验 ---------- */

    @GetMapping("/fai/list")
    public Result<List<FaiRecord>> faiList(@RequestParam(required = false) Long workOrderId) {
        return Result.ok(faiMapper.selectList(new LambdaQueryWrapper<FaiRecord>()
                .eq(workOrderId != null, FaiRecord::getWorkOrderId, workOrderId)
                .orderByDesc(FaiRecord::getId).last("LIMIT 100")));
    }

    @SaCheckRole(value = {"admin", "quality"}, mode = SaMode.OR)
    @PostMapping("/fai/save")
    public Result<Void> faiSave(@RequestBody FaiRecord record) {
        if (record.getId() == null) {
            faiMapper.insert(record);
        } else {
            faiMapper.updateById(record);
        }
        return Result.ok();
    }
}
