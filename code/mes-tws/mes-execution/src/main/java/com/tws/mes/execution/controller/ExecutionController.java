package com.tws.mes.execution.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tws.mes.common.result.PageResult;
import com.tws.mes.common.result.Result;
import com.tws.mes.execution.dto.CheckinDTO;
import com.tws.mes.execution.entity.SnBinding;
import com.tws.mes.execution.entity.SnRegistry;
import com.tws.mes.execution.entity.StationLog;
import com.tws.mes.execution.service.CheckinService;
import com.tws.mes.execution.service.SnService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 现场执行接口：过站 / 绑定 / 上料 / SN / 流水 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ExecutionController {

    private final CheckinService checkinService;
    private final SnService snService;

    /* ---------- 过站（扫码枪/模拟器） ---------- */

    @SaCheckRole(value = {"admin", "operator", "quality"}, mode = cn.dev33.satoken.annotation.SaMode.OR)
    @PostMapping("/station/checkin")
    public Result<Map<String, Object>> checkin(@RequestBody CheckinDTO dto) {
        return Result.ok(checkinService.checkin(dto));
    }

    /** 模拟器辅助：工单+工位 → 工序快照规则要点（动态渲染测试项输入） */
    @GetMapping("/station/context")
    public Result<Map<String, Object>> stationContext(@RequestParam Long woId,
                                                      @RequestParam String stationCode) {
        return Result.ok(checkinService.stationContext(woId, stationCode));
    }

    /* ---------- 绑定 / 上料 ---------- */

    @Data
    public static class BindDTO {
        private String machineSn;
        private String stationCode;
        private List<SnService.BindItem> children;
    }

    @SaCheckRole(value = {"admin", "operator", "quality"}, mode = cn.dev33.satoken.annotation.SaMode.OR)
    @PostMapping("/station/bind")
    public Result<Void> bind(@RequestBody BindDTO dto) {
        snService.bind(dto.getMachineSn(), dto.getStationCode(), dto.getChildren());
        return Result.ok();
    }

    @SaCheckRole(value = {"admin", "operator", "quality"}, mode = cn.dev33.satoken.annotation.SaMode.OR)
    @PostMapping("/station/loading")
    public Result<Void> loading(@RequestBody Map<String, String> body) {
        snService.loading(body.get("stationCode"), body.get("batchNo"));
        return Result.ok();
    }

    /* ---------- SN ---------- */

    @GetMapping("/sn/page")
    public Result<PageResult<SnRegistry>> snPage(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Long workOrderId,
                                                 @RequestParam(required = false) String snType,
                                                 @RequestParam(required = false) String status) {
        return Result.ok(snService.page(page, size, keyword, workOrderId, snType, status));
    }

    /** 模拟器辅助：取下一个待过站 SN */
    @GetMapping("/sn/next")
    public Result<SnRegistry> nextSn(@RequestParam Long workOrderId,
                                     @RequestParam(required = false) Integer beforeSeq,
                                     @RequestParam(required = false) String snType) {
        return Result.ok(snService.nextSn(workOrderId, beforeSeq, snType));
    }

    @SaCheckRole(value = {"admin", "planner"}, mode = cn.dev33.satoken.annotation.SaMode.OR)
    @PostMapping("/sn/generate-machine")
    public Result<Integer> generateMachine(@RequestBody Map<String, Long> body) {
        return Result.ok(snService.generateMachineSn(body.get("workOrderId")));
    }

    @Data
    public static class ComponentDTO {
        private String snType;
        private String batchNo;
        private Integer count;
    }

    @SaCheckRole(value = {"admin", "operator", "quality"}, mode = cn.dev33.satoken.annotation.SaMode.OR)
    @PostMapping("/sn/register-component")
    public Result<Integer> registerComponent(@RequestBody ComponentDTO dto) {
        return Result.ok(snService.registerComponent(dto.getSnType(), dto.getBatchNo(), dto.getCount()));
    }

    @GetMapping("/sn/bindings")
    public Result<List<SnBinding>> bindings(@RequestParam String parentSn) {
        return Result.ok(snService.bindingsOf(parentSn));
    }

    /* ---------- 流水 ---------- */

    @GetMapping("/log/page")
    public Result<PageResult<StationLog>> logPage(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String sn,
                                                  @RequestParam(required = false) Long workOrderId,
                                                  @RequestParam(required = false) String stationCode,
                                                  @RequestParam(required = false) String recordType) {
        return Result.ok(snService.logPage(page, size, sn, workOrderId, stationCode, recordType));
    }
}
