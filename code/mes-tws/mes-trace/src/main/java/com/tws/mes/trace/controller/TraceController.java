package com.tws.mes.trace.controller;

import com.tws.mes.common.result.Result;
import com.tws.mes.trace.entity.TraceTask;
import com.tws.mes.trace.service.TraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 追溯管理接口 */
@RestController
@RequestMapping("/api/trace")
@RequiredArgsConstructor
public class TraceController {

    private final TraceService traceService;

    /** 正向：SN → 过站时间线 + 绑定 + 不良维修 + 批次 */
    @GetMapping("/forward/{sn}")
    public Result<Map<String, Object>> forward(@PathVariable String sn) {
        return Result.ok(traceService.forward(sn));
    }

    /** 反向：批次 → 部件 SN → 受影响整机 */
    @GetMapping("/reverse/{batchNo}")
    public Result<Map<String, Object>> reverse(@PathVariable String batchNo) {
        return Result.ok(traceService.reverse(batchNo));
    }

    @GetMapping("/tasks")
    public Result<List<TraceTask>> tasks() {
        return Result.ok(traceService.recentTasks());
    }
}
