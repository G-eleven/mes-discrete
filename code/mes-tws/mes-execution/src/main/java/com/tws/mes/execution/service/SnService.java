package com.tws.mes.execution.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tws.mes.base.entity.MdMaterialBatch;
import com.tws.mes.base.mapper.MdMaterialBatchMapper;
import com.tws.mes.base.mapper.MdStationMapper;
import com.tws.mes.base.entity.MdStation;
import com.tws.mes.common.constant.MesConst;
import com.tws.mes.common.exception.BizException;
import com.tws.mes.common.result.PageResult;
import com.tws.mes.execution.entity.PlanWorkOrder;
import com.tws.mes.execution.entity.SnBinding;
import com.tws.mes.execution.entity.SnRegistry;
import com.tws.mes.execution.entity.StationLog;
import com.tws.mes.execution.mapper.PlanWorkOrderMapper;
import com.tws.mes.execution.mapper.SnBindingMapper;
import com.tws.mes.execution.mapper.SnRegistryMapper;
import com.tws.mes.execution.mapper.StationLogMapper;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import com.tws.mes.base.service.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SN 服务 + 绑定 + 上料（同属现场执行操作）。
 */
@Service
@RequiredArgsConstructor
public class SnService {

    private final SnRegistryMapper snMapper;
    private final PlanWorkOrderMapper woMapper;
    private final SnBindingMapper bindingMapper;
    private final StationLogMapper logMapper;
    private final MdStationMapper stationMapper;
    private final MdMaterialBatchMapper batchMapper;
    private final CurrentUserService currentUserService;

    /* -------------------- 查询 -------------------- */

    public PageResult<SnRegistry> page(long page, long size, String keyword, Long workOrderId, String snType, String status) {
        LambdaQueryWrapper<SnRegistry> qw = new LambdaQueryWrapper<SnRegistry>()
                .and(StrUtil.isNotBlank(keyword), w -> w.like(SnRegistry::getSn, keyword))
                .eq(workOrderId != null, SnRegistry::getWorkOrderId, workOrderId)
                .eq(StrUtil.isNotBlank(snType), SnRegistry::getSnType, snType)
                .eq(StrUtil.isNotBlank(status), SnRegistry::getStatus, status)
                .orderByDesc(SnRegistry::getId);
        Page<SnRegistry> p = snMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    /** 模拟器辅助：某工单下"下一个待过某工序"的 SN（减少手输） */
    public SnRegistry nextSn(Long workOrderId, Integer beforeSeq, String snType) {
        LambdaQueryWrapper<SnRegistry> qw = new LambdaQueryWrapper<SnRegistry>()
                .eq(SnRegistry::getWorkOrderId, workOrderId)
                .eq(StrUtil.isNotBlank(snType), SnRegistry::getSnType, snType)
                .in(SnRegistry::getStatus, MesConst.SN_INIT, MesConst.SN_IN_LINE, MesConst.SN_RETEST);
        if (beforeSeq != null) {
            qw.lt(SnRegistry::getCurrentSeq, beforeSeq);
        }
        qw.orderByAsc(SnRegistry::getId).last("LIMIT 1");
        return snMapper.selectOne(qw);
    }

    /* -------------------- 生成 -------------------- */

    /** 整机 SN 按工单批量生成：WO...-0001 ~ WO...-XXXX（幂等：已生成则拒绝） */
    @Transactional
    public int generateMachineSn(Long workOrderId) {
        PlanWorkOrder wo = woMapper.selectById(workOrderId);
        if (wo == null) throw new BizException("工单不存在");
        if (wo.getSnGenerated() != null && wo.getSnGenerated() == 1) {
            throw new BizException("该工单整机 SN 已生成过，不可重复生成");
        }
        List<SnRegistry> batch = new ArrayList<>();
        for (int i = 1; i <= wo.getPlanQty(); i++) {
            SnRegistry sn = new SnRegistry();
            sn.setSn(wo.getWoNo() + "-" + String.format("%04d", i));
            sn.setSnType("MACHINE");
            sn.setWorkOrderId(wo.getId());
            sn.setCurrentSeq(0);
            sn.setStatus(MesConst.SN_INIT);
            batch.add(sn);
            // 学习版逐条 insert 便于阅读；生产用 MyBatis-Plus 批量/rewriteBatchedStatements
            snMapper.insert(sn);
        }
        wo.setSnGenerated(1);
        woMapper.updateById(wo);
        return batch.size();
    }

    /** 部件 SN 批量注册（来料入库即注册：左耳/右耳/盒，可带批次号） */
    @Transactional
    public int registerComponent(String snType, String batchNo, Integer count) {
        if (!"LEFT".equals(snType) && !"RIGHT".equals(snType) && !"CASE".equals(snType)) {
            throw new BizException("部件类型仅支持 LEFT/RIGHT/CASE");
        }
        if (StrUtil.isNotBlank(batchNo)) {
            Long cnt = batchMapper.selectCount(new LambdaQueryWrapper<MdMaterialBatch>()
                    .eq(MdMaterialBatch::getBatchNo, batchNo));
            if (cnt == null || cnt == 0) throw new BizException("批次号未登记: " + batchNo);
        }
        String prefix = snType + "-" + (StrUtil.blankToDefault(batchNo, "NOBATCH")) + "-";
        int seqStart = snMapper.selectList(new LambdaQueryWrapper<SnRegistry>()
                        .likeRight(SnRegistry::getSn, prefix)).size();
        for (int i = 1; i <= count; i++) {
            SnRegistry sn = new SnRegistry();
            sn.setSn(prefix + String.format("%04d", seqStart + i));
            sn.setSnType(snType);
            sn.setBatchNo(StrUtil.blankToDefault(batchNo, null));
            sn.setCurrentSeq(0);
            sn.setStatus(MesConst.SN_INIT);
            snMapper.insert(sn);
        }
        return count;
    }

    /* -------------------- 绑定 -------------------- */

    @Data
    public static class BindItem {
        private String sn;
        private String bindType;
    }

    /** 三码绑定：把 LEFT/RIGHT/CASE 子件绑到整机 SN（uk(child_sn,bind_type) 防重绑） */
    @Transactional
    public void bind(String machineSn, String stationCode, List<BindItem> children) {
        MdStation station = stationMapper.selectOne(new LambdaQueryWrapper<MdStation>()
                .eq(MdStation::getStationCode, stationCode));
        if (station == null) throw new BizException("工位不存在: " + stationCode);
        SnRegistry machine = snMapper.selectOne(new LambdaQueryWrapper<SnRegistry>()
                .eq(SnRegistry::getSn, machineSn));
        if (machine == null || !"MACHINE".equals(machine.getSnType())) {
            throw new BizException("整机 SN 不存在: " + machineSn);
        }
        if (machine.getWorkOrderId() == null) throw new BizException("整机 SN 未关联工单");
        PlanWorkOrder wo = woMapper.selectById(machine.getWorkOrderId());
        if (wo == null || !MesConst.WO_IN_PROGRESS.equals(wo.getStatus())) {
            throw new BizException("工单未处于生产中，不能绑定");
        }
        if (children == null || children.isEmpty()) throw new BizException("至少绑定一个子件");
        String operator = currentUserService.currentUsername();
        List<String> childSns = new ArrayList<>();
        for (BindItem item : children) {
            SnRegistry child = snMapper.selectOne(new LambdaQueryWrapper<SnRegistry>()
                    .eq(SnRegistry::getSn, item.getSn()));
            if (child == null) throw new BizException("子件 SN 不存在: " + item.getSn());
            if (!item.getBindType().equals(child.getSnType())) {
                throw new BizException(String.format("子件 %s 类型为 %s，与绑定类型 %s 不符",
                        item.getSn(), child.getSnType(), item.getBindType()));
            }
            if (MesConst.SN_SCRAP.equals(child.getStatus())) throw new BizException("子件已报废: " + item.getSn());
            Long bound = bindingMapper.selectCount(new LambdaQueryWrapper<SnBinding>()
                    .eq(SnBinding::getChildSn, item.getSn())
                    .eq(SnBinding::getBindType, item.getBindType()));
            if (bound != null && bound > 0) throw new BizException("子件已绑定过其他父SN: " + item.getSn());
            SnBinding b = new SnBinding();
            b.setParentSn(machineSn);
            b.setChildSn(item.getSn());
            b.setBindType(item.getBindType());
            b.setWorkOrderId(wo.getId());
            b.setStationCode(stationCode);
            b.setOperator(operator);
            bindingMapper.insert(b);
            child.setParentSn(machineSn);
            snMapper.updateById(child);
            childSns.add(item.getSn());
        }
        // 绑定也是一条流水（BINDING），追溯"何时何地绑定"
        StationLog logRow = new StationLog();
        logRow.setSn(machineSn);
        logRow.setWorkOrderId(wo.getId());
        logRow.setStationCode(stationCode);
        logRow.setOperationCode(station.getOperationCode());
        logRow.setRecordType(MesConst.RT_BINDING);
        logRow.setTestData("{\"children\":" + JSONUtil.toJsonStr(childSns) + "}");
        logRow.setRetestRound(0);
        logRow.setOperator(operator);
        logMapper.insert(logRow);
        if (MesConst.SN_INIT.equals(machine.getStatus())) {
            machine.setStatus(MesConst.SN_IN_LINE);
            snMapper.updateById(machine);
        }
    }

    /* -------------------- 上料 -------------------- */

    /** 工位上料：扫批次 → 记 LOADING 流水（批次冻结则拦截） */
    public void loading(String stationCode, String batchNo) {
        MdStation station = stationMapper.selectOne(new LambdaQueryWrapper<MdStation>()
                .eq(MdStation::getStationCode, stationCode));
        if (station == null) throw new BizException("工位不存在: " + stationCode);
        MdMaterialBatch batch = batchMapper.selectOne(new LambdaQueryWrapper<MdMaterialBatch>()
                .eq(MdMaterialBatch::getBatchNo, batchNo));
        if (batch == null) throw new BizException("批次未登记: " + batchNo);
        if (batch.getStatus() == 0) throw new BizException("批次已冻结，禁止上料: " + batchNo);
        StationLog logRow = new StationLog();
        logRow.setSn("LOADING-" + System.currentTimeMillis());
        logRow.setStationCode(stationCode);
        logRow.setOperationCode(station.getOperationCode());
        logRow.setRecordType(MesConst.RT_LOADING);
        logRow.setBatchNo(batchNo);
        logRow.setRetestRound(0);
        logRow.setOperator(currentUserService.currentUsername());
        logMapper.insert(logRow);
    }

    /** 流水查询（过站/上料/绑定） */
    public PageResult<StationLog> logPage(long page, long size, String sn, Long workOrderId,
                                          String stationCode, String recordType) {
        LambdaQueryWrapper<StationLog> qw = new LambdaQueryWrapper<StationLog>()
                .like(StrUtil.isNotBlank(sn), StationLog::getSn, sn)
                .eq(workOrderId != null, StationLog::getWorkOrderId, workOrderId)
                .eq(StrUtil.isNotBlank(stationCode), StationLog::getStationCode, stationCode)
                .eq(StrUtil.isNotBlank(recordType), StationLog::getRecordType, recordType)
                .orderByDesc(StationLog::getId);
        Page<StationLog> p = logMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    /** 绑定关系查询（按父 SN） */
    public List<SnBinding> bindingsOf(String parentSn) {
        return bindingMapper.selectList(new LambdaQueryWrapper<SnBinding>()
                .eq(SnBinding::getParentSn, parentSn));
    }
}
