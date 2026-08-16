package com.tws.mes.trace.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tws.mes.base.entity.MdMaterialBatch;
import com.tws.mes.base.mapper.MdMaterialBatchMapper;
import com.tws.mes.base.service.CurrentUserService;
import com.tws.mes.common.exception.BizException;
import com.tws.mes.execution.entity.*;
import com.tws.mes.execution.mapper.*;
import com.tws.mes.trace.entity.TraceTask;
import com.tws.mes.trace.mapper.TraceTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 追溯管理：正向（SN → 全档案）与反向（批次 → 受影响整机）。
 *
 * 业务价值（对齐叙事）：客户审厂要求 SN 级追溯报告，人工翻纸质流转卡 2 天；
 * 系统上线后输入 SN/批次秒级出全链路档案。
 */
@Service
@RequiredArgsConstructor
public class TraceService {

    private final SnRegistryMapper snMapper;
    private final SnBindingMapper bindingMapper;
    private final StationLogMapper logMapper;
    private final DefectRecordMapper defectMapper;
    private final RepairRecordMapper repairMapper;
    private final PlanWorkOrderMapper woMapper;
    private final MdMaterialBatchMapper batchMapper;
    private final TraceTaskMapper taskMapper;
    private final CurrentUserService currentUserService;

    /* ================= 正向追溯：SN → 全档案 ================= */

    public Map<String, Object> forward(String sn) {
        long start = System.currentTimeMillis();
        SnRegistry reg = snMapper.selectOne(new LambdaQueryWrapper<SnRegistry>().eq(SnRegistry::getSn, sn));
        if (reg == null) throw new BizException("SN 不存在: " + sn);

        Map<String, Object> r = new HashMap<>();
        r.put("sn", reg);
        PlanWorkOrder wo = reg.getWorkOrderId() == null ? null : woMapper.selectById(reg.getWorkOrderId());
        r.put("woNo", wo == null ? null : wo.getWoNo());

        // 过站时间线（含测试数据/轮次/NG）
        List<StationLog> logs = logMapper.selectList(new LambdaQueryWrapper<StationLog>()
                .eq(StationLog::getSn, sn)
                .and(w -> w.eq(StationLog::getRecordType, "CHECKIN")
                        .or().eq(StationLog::getRecordType, "BINDING"))
                .orderByAsc(StationLog::getCreateTime));
        r.put("timeline", logs);

        // 绑定关系：整机看子件（含批次），部件看父链
        if ("MACHINE".equals(reg.getSnType())) {
            List<SnBinding> bindings = bindingMapper.selectList(
                    new LambdaQueryWrapper<SnBinding>().eq(SnBinding::getParentSn, sn));
            List<Map<String, Object>> children = new ArrayList<>();
            for (SnBinding b : bindings) {
                Map<String, Object> c = new HashMap<>();
                c.put("bindType", b.getBindType());
                c.put("childSn", b.getChildSn());
                c.put("bindTime", b.getCreateTime());
                c.put("stationCode", b.getStationCode());
                SnRegistry child = snMapper.selectOne(new LambdaQueryWrapper<SnRegistry>()
                        .eq(SnRegistry::getSn, b.getChildSn()));
                if (child != null) {
                    c.put("batchNo", child.getBatchNo());
                    c.put("childStatus", child.getStatus());
                }
                children.add(c);
            }
            r.put("children", children);
        } else if (reg.getParentSn() != null) {
            SnRegistry parent = snMapper.selectOne(new LambdaQueryWrapper<SnRegistry>()
                    .eq(SnRegistry::getSn, reg.getParentSn()));
            r.put("parent", parent);
        }

        // 不良与维修史
        List<DefectRecord> defects = defectMapper.selectList(
                new LambdaQueryWrapper<DefectRecord>().eq(DefectRecord::getSn, sn).orderByAsc(DefectRecord::getId));
        r.put("defects", defects);
        List<Map<String, Object>> repairs = new ArrayList<>();
        for (DefectRecord d : defects) {
            List<RepairRecord> rs = repairMapper.selectList(
                    new LambdaQueryWrapper<RepairRecord>().eq(RepairRecord::getDefectId, d.getId()));
            for (RepairRecord rr : rs) {
                Map<String, Object> m = new HashMap<>();
                m.put("defectCode", d.getDefectCode());
                m.put("repair", rr);
                repairs.add(m);
            }
        }
        r.put("repairs", repairs);

        saveTask("FORWARD", sn, logs.size() + defects.size());
        r.put("costMs", (int) (System.currentTimeMillis() - start));
        return r;
    }

    /* ================= 反向追溯：批次 → 受影响整机 ================= */

    public Map<String, Object> reverse(String batchNo) {
        long start = System.currentTimeMillis();
        MdMaterialBatch batch = batchMapper.selectOne(
                new LambdaQueryWrapper<MdMaterialBatch>().eq(MdMaterialBatch::getBatchNo, batchNo));
        if (batch == null) throw new BizException("批次不存在: " + batchNo);

        // 批次 → 部件 SN → 父整机
        List<SnRegistry> parts = snMapper.selectList(new LambdaQueryWrapper<SnRegistry>()
                .eq(SnRegistry::getBatchNo, batchNo));
        List<Map<String, Object>> machines = new ArrayList<>();
        int unbound = 0;
        for (SnRegistry part : parts) {
            if (part.getParentSn() == null) {
                if (!"MACHINE".equals(part.getSnType())) unbound++;
                continue;
            }
            SnRegistry machine = snMapper.selectOne(new LambdaQueryWrapper<SnRegistry>()
                    .eq(SnRegistry::getSn, part.getParentSn()));
            if (machine == null) continue;
            Long defectCnt = defectMapper.selectCount(new LambdaQueryWrapper<DefectRecord>()
                    .eq(DefectRecord::getSn, machine.getSn()));
            PlanWorkOrder wo = machine.getWorkOrderId() == null ? null : woMapper.selectById(machine.getWorkOrderId());
            Map<String, Object> m = new HashMap<>();
            m.put("partSn", part.getSn());
            m.put("partType", part.getSnType());
            m.put("machine", machine);
            m.put("woNo", wo == null ? null : wo.getWoNo());
            m.put("defectCnt", defectCnt == null ? 0 : defectCnt);
            machines.add(m);
        }

        Map<String, Object> r = new HashMap<>();
        r.put("batch", batch);
        r.put("parts", parts);
        r.put("machines", machines);
        r.put("unbound", unbound);
        r.put("total", parts.size());
        saveTask("REVERSE", batchNo, machines.size());
        r.put("costMs", (int) (System.currentTimeMillis() - start));
        return r;
    }

    public List<TraceTask> recentTasks() {
        return taskMapper.selectList(new LambdaQueryWrapper<TraceTask>()
                .orderByDesc(TraceTask::getId).last("LIMIT 20"));
    }

    private void saveTask(String type, String key, int resultCount) {
        TraceTask t = new TraceTask();
        t.setTaskNo("TR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        t.setTraceType(type);
        t.setQueryKey(key);
        t.setResultCount(resultCount);
        t.setCreateBy(currentUserService.currentUsername());
        taskMapper.insert(t);
    }
}
