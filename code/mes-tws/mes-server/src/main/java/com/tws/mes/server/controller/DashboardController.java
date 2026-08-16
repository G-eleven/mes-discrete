package com.tws.mes.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tws.mes.common.result.Result;
import com.tws.mes.execution.entity.DefectRecord;
import com.tws.mes.execution.entity.PlanWorkOrder;
import com.tws.mes.execution.entity.StationLog;
import com.tws.mes.execution.mapper.DefectRecordMapper;
import com.tws.mes.execution.mapper.PlanWorkOrderMapper;
import com.tws.mes.execution.mapper.StationLogMapper;
import com.tws.mes.quality.service.FpyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 总览页聚合数据（跨模块查询，放在聚合模块 mes-server） */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final PlanWorkOrderMapper woMapper;
    private final StationLogMapper logMapper;
    private final DefectRecordMapper defectMapper;
    private final FpyService fpyService;

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        Map<String, Object> r = new HashMap<>();
        r.put("inProgressWo", woMapper.selectCount(
                new LambdaQueryWrapper<PlanWorkOrder>().eq(PlanWorkOrder::getStatus, "IN_PROGRESS")));
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        r.put("todayCheckin", logMapper.selectCount(new LambdaQueryWrapper<StationLog>()
                .eq(StationLog::getRecordType, "CHECKIN").ge(StationLog::getCreateTime, todayStart)));
        r.put("openDefects", defectMapper.selectCount(
                new LambdaQueryWrapper<DefectRecord>().eq(DefectRecord::getStatus, "OPEN")));
        r.put("fpy", fpyService.summary(null).get("fpy"));
        List<PlanWorkOrder> wos = woMapper.selectList(new LambdaQueryWrapper<PlanWorkOrder>()
                .eq(PlanWorkOrder::getStatus, "IN_PROGRESS").orderByDesc(PlanWorkOrder::getId));
        r.put("woProgress", wos);
        r.put("recentLogs", logMapper.selectList(new LambdaQueryWrapper<StationLog>()
                .eq(StationLog::getRecordType, "CHECKIN").orderByDesc(StationLog::getId).last("LIMIT 8")));
        return Result.ok(r);
    }
}
