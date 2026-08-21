package com.tws.mes.execution.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tws.mes.base.entity.MdBomItem;
import com.tws.mes.base.entity.MdMaterial;
import com.tws.mes.base.entity.MdMaterialBatch;
import com.tws.mes.base.entity.MdStation;
import com.tws.mes.base.mapper.MdBomItemMapper;
import com.tws.mes.base.mapper.MdMaterialBatchMapper;
import com.tws.mes.base.mapper.MdMaterialMapper;
import com.tws.mes.base.mapper.MdStationMapper;
import com.tws.mes.base.service.CurrentUserService;
import com.tws.mes.common.constant.MesConst;
import com.tws.mes.common.exception.BizException;
import com.tws.mes.execution.entity.PlanWorkOrder;
import com.tws.mes.execution.entity.StationLoading;
import com.tws.mes.execution.entity.StationLog;
import com.tws.mes.execution.mapper.PlanWorkOrderMapper;
import com.tws.mes.execution.mapper.StationLoadingMapper;
import com.tws.mes.execution.mapper.StationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工位上料服务 —— 把"上料"从"只记一条流水"升级为"齐套计算 + 上料防错 + 台账管理"闭环。
 *
 * 三大能力：
 * 1. kitting 齐套计算：工单+工位 → 按 MBOM 算出应上物料清单、已上量、缺口、状态（OK/SHORT/LOW_WARN）
 * 2. loading 上料防错：扫批次 → 校验"批次物料 == 工位MBOM需求"且批次未冻结 → 写台账 + 记流水
 * 3. ledger 台账查询：当前工位已上批次 + 剩余量 + 低量预警标记
 *
 * shortMaterials 供 LoadingRule 在过站规则链里按物料维度校验齐套。
 */
@Service
@RequiredArgsConstructor
public class LoadingService {

    /** 低量预警阈值：剩余量低于上料量 12% 标黄 */
    private static final double LOW_WARN_RATIO = 0.12;

    private final StationLoadingMapper loadingLedgerMapper;
    private final MdBomItemMapper bomItemMapper;
    private final MdMaterialMapper materialMapper;
    private final MdMaterialBatchMapper batchMapper;
    private final MdStationMapper stationMapper;
    private final PlanWorkOrderMapper woMapper;
    private final StationLogMapper logMapper;
    private final CurrentUserService currentUserService;

    /* ==================== 齐套计算 ==================== */

    /** 齐套：工单+工位 → 应上物料清单 + 已上量 + 缺口 + 状态 */
    public Map<String, Object> kitting(Long woId, String stationCode) {
        MdStation station = loadStation(stationCode);
        PlanWorkOrder wo = woMapper.selectById(woId);
        if (wo == null) throw new BizException("工单不存在");
        List<MdBomItem> bomItems = bomItemMapper.selectList(new LambdaQueryWrapper<MdBomItem>()
                .eq(MdBomItem::getBomId, wo.getBomId())
                .eq(MdBomItem::getOperationCode, station.getOperationCode()));
        return assembleKitting(woId, wo, station, bomItems);
    }

    /** 看板：工单所有需上料工位的齐套汇总（按工序分组，每工序取其启用工位） */
    public List<Map<String, Object>> board(Long woId) {
        PlanWorkOrder wo = woMapper.selectById(woId);
        if (wo == null) throw new BizException("工单不存在");
        List<MdBomItem> items = bomItemMapper.selectList(new LambdaQueryWrapper<MdBomItem>()
                .eq(MdBomItem::getBomId, wo.getBomId())
                .isNotNull(MdBomItem::getOperationCode));
        Map<String, List<MdBomItem>> byOp = new LinkedHashMap<>();
        for (MdBomItem it : items) {
            byOp.computeIfAbsent(it.getOperationCode(), k -> new ArrayList<>()).add(it);
        }
        List<Map<String, Object>> stations = new ArrayList<>();
        for (Map.Entry<String, List<MdBomItem>> e : byOp.entrySet()) {
            List<MdStation> stList = stationMapper.selectList(new LambdaQueryWrapper<MdStation>()
                    .eq(MdStation::getOperationCode, e.getKey()).eq(MdStation::getStatus, 1));
            for (MdStation st : stList) {
                stations.add(assembleKitting(woId, wo, st, e.getValue()));
            }
        }
        return stations;
    }

    private Map<String, Object> assembleKitting(Long woId, PlanWorkOrder wo, MdStation station, List<MdBomItem> bomItems) {
        List<Map<String, Object>> items = new ArrayList<>();
        int total = 0, loaded = 0, shortCnt = 0, lowCnt = 0;
        for (MdBomItem it : bomItems) {
            MdMaterial m = materialMapper.selectById(it.getChildMaterialId());
            int requiredQty = (int) Math.ceil(it.getQuantity() * wo.getPlanQty());
            List<StationLoading> ledger = activeLedger(woId, station.getStationCode(), it.getChildMaterialId());
            int loadedQty = ledger.stream().mapToInt(l -> n(l.getLoadingQty())).sum();
            int remainQty = ledger.stream().mapToInt(l -> n(l.getRemainQty())).sum();
            int shortQty = Math.max(0, requiredQty - loadedQty);
            String status;
            if (ledger.isEmpty()) {
                status = "SHORT";
                shortCnt++;
            } else if (loadedQty > 0 && remainQty / (double) loadedQty < LOW_WARN_RATIO) {
                status = "LOW_WARN";
                lowCnt++;
            } else {
                status = "OK";
                loaded++;
            }
            total++;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("materialId", it.getChildMaterialId());
            row.put("materialCode", m != null ? m.getMaterialCode() : null);
            row.put("materialName", m != null ? m.getMaterialName() : null);
            row.put("spec", m != null ? m.getSpec() : null);
            row.put("unit", m != null ? m.getUnit() : null);
            row.put("requiredQty", requiredQty);
            row.put("loadedQty", loadedQty);
            row.put("remainQty", remainQty);
            row.put("shortQty", shortQty);
            row.put("status", status);
            items.add(row);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("workOrderId", woId);
        r.put("woNo", wo.getWoNo());
        r.put("stationCode", station.getStationCode());
        r.put("stationName", station.getStationName());
        r.put("operationCode", station.getOperationCode());
        r.put("items", items);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", total);
        summary.put("loaded", loaded);
        summary.put("short", shortCnt);
        summary.put("lowWarn", lowCnt);
        r.put("summary", summary);
        return r;
    }

    /* ==================== 上料防错 ==================== */

    /** 上料：校验批次物料匹配工位 MBOM + 批次未冻结 → 写台账 + 记 LOADING 流水 */
    @Transactional
    public StationLoading loading(Long woId, String stationCode, Long materialId, String batchNo, Integer loadingQty) {
        MdStation station = loadStation(stationCode);
        PlanWorkOrder wo = woMapper.selectById(woId);
        if (wo == null) throw new BizException("工单不存在");
        if (!MesConst.WO_IN_PROGRESS.equals(wo.getStatus())) {
            throw new BizException("工单未处于生产中，不能上料");
        }
        MdMaterialBatch batch = batchMapper.selectOne(new LambdaQueryWrapper<MdMaterialBatch>()
                .eq(MdMaterialBatch::getBatchNo, batchNo));
        if (batch == null) throw new BizException("批次未登记: " + batchNo);
        if (batch.getStatus() == 0) throw new BizException("批次已冻结，禁止上料: " + batchNo);
        // 防错1：扫的批次物料编码必须等于声明要上的物料
        if (!batch.getMaterialId().equals(materialId)) {
            MdMaterial expect = materialMapper.selectById(materialId);
            MdMaterial actual = materialMapper.selectById(batch.getMaterialId());
            throw new BizException(String.format("物料不匹配：应上 %s，实扫批次 %s 属于 %s",
                    expect != null ? expect.getMaterialCode() : materialId, batchNo,
                    actual != null ? actual.getMaterialCode() : batch.getMaterialId()));
        }
        // 防错2：该物料必须属于本工位 MBOM 投料清单（防止上错工位/上错料）
        Long cnt = bomItemMapper.selectCount(new LambdaQueryWrapper<MdBomItem>()
                .eq(MdBomItem::getBomId, wo.getBomId())
                .eq(MdBomItem::getOperationCode, station.getOperationCode())
                .eq(MdBomItem::getChildMaterialId, materialId));
        if (cnt == null || cnt == 0) {
            MdMaterial m = materialMapper.selectById(materialId);
            throw new BizException(String.format("物料 %s 不属于工位 %s 的投料清单，禁止上料",
                    m != null ? m.getMaterialCode() : materialId, stationCode));
        }
        int qty = loadingQty != null ? loadingQty : n(batch.getRemainQty());
        String operator = currentUserService.currentUsername();
        // 写台账
        StationLoading row = new StationLoading();
        row.setWorkOrderId(woId);
        row.setStationCode(stationCode);
        row.setOperationCode(station.getOperationCode());
        row.setMaterialId(materialId);
        row.setBatchNo(batchNo);
        row.setLoadingQty(qty);
        row.setRemainQty(qty);
        row.setStatus("ACTIVE");
        row.setOperator(operator);
        loadingLedgerMapper.insert(row);
        // 兼容旧 LOADING 流水（追溯与历史看板沿用同一份流水）
        MdMaterial m = materialMapper.selectById(materialId);
        StationLog logRow = new StationLog();
        logRow.setSn("LOADING-" + System.currentTimeMillis());
        logRow.setWorkOrderId(woId);
        logRow.setStationCode(stationCode);
        logRow.setOperationCode(station.getOperationCode());
        logRow.setRecordType(MesConst.RT_LOADING);
        logRow.setBatchNo(batchNo);
        Map<String, Object> td = new LinkedHashMap<>();
        td.put("materialId", materialId);
        td.put("materialCode", m != null ? m.getMaterialCode() : null);
        td.put("loadingQty", qty);
        logRow.setTestData(JSONUtil.toJsonStr(td));
        logRow.setRetestRound(0);
        logRow.setOperator(operator);
        logMapper.insert(logRow);
        return row;
    }

    /* ==================== 台账查询 ==================== */

    /** 当前工位已上批次台账（看板右栏 + 退料参考） */
    public List<Map<String, Object>> ledger(Long woId, String stationCode) {
        List<StationLoading> rows = loadingLedgerMapper.selectList(new LambdaQueryWrapper<StationLoading>()
                .eq(woId != null, StationLoading::getWorkOrderId, woId)
                .eq(StationLoading::getStationCode, stationCode)
                .eq(StationLoading::getStatus, "ACTIVE")
                .orderByDesc(StationLoading::getId));
        List<Map<String, Object>> list = new ArrayList<>();
        for (StationLoading l : rows) {
            MdMaterial m = materialMapper.selectById(l.getMaterialId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", l.getId());
            row.put("materialId", l.getMaterialId());
            row.put("materialCode", m != null ? m.getMaterialCode() : null);
            row.put("materialName", m != null ? m.getMaterialName() : null);
            row.put("batchNo", l.getBatchNo());
            row.put("loadingQty", l.getLoadingQty());
            row.put("remainQty", l.getRemainQty());
            row.put("lowWarn", l.getLoadingQty() != null && l.getLoadingQty() > 0
                    && n(l.getRemainQty()) / (double) l.getLoadingQty() < LOW_WARN_RATIO);
            row.put("operator", l.getOperator());
            row.put("createTime", l.getCreateTime());
            list.add(row);
        }
        return list;
    }

    /* ==================== 供规则链调用 ==================== */

    /** 返回该工位缺料的物料编码清单（空列表 = 齐套）；供 LoadingRule 在过站时校验 */
    public List<String> shortMaterials(Long woId, String stationCode, String operationCode, Long bomId) {
        List<MdBomItem> items = bomItemMapper.selectList(new LambdaQueryWrapper<MdBomItem>()
                .eq(MdBomItem::getBomId, bomId)
                .eq(MdBomItem::getOperationCode, operationCode));
        List<String> shortList = new ArrayList<>();
        for (MdBomItem it : items) {
            Long cnt = loadingLedgerMapper.selectCount(new LambdaQueryWrapper<StationLoading>()
                    .eq(StationLoading::getWorkOrderId, woId)
                    .eq(StationLoading::getStationCode, stationCode)
                    .eq(StationLoading::getMaterialId, it.getChildMaterialId())
                    .eq(StationLoading::getStatus, "ACTIVE"));
            if (cnt == null || cnt == 0) {
                MdMaterial m = materialMapper.selectById(it.getChildMaterialId());
                shortList.add(m != null ? m.getMaterialCode() : String.valueOf(it.getChildMaterialId()));
            }
        }
        return shortList;
    }

    /* ==================== 过站消耗扣减 ==================== */

    /**
     * 过站 OK 时按 MBOM 定额扣减本工位上料台账剩余量 + 批次库存（消耗闭环）。
     * FIFO：按上料时间先后扣减，先上的先扣；台账扣完仍不够则跳过
     * （齐套校验只校验"有无 ACTIVE 记录"不校验数量，数量不足留待人工补料/预警）。
     */
    public void consume(Long woId, String stationCode, String operationCode, Long bomId) {
        List<MdBomItem> items = bomItemMapper.selectList(new LambdaQueryWrapper<MdBomItem>()
                .eq(MdBomItem::getBomId, bomId)
                .eq(MdBomItem::getOperationCode, operationCode));
        for (MdBomItem it : items) {
            int need = (int) Math.ceil(it.getQuantity());
            if (need <= 0) continue;
            List<StationLoading> ledger = loadingLedgerMapper.selectList(new LambdaQueryWrapper<StationLoading>()
                    .eq(StationLoading::getWorkOrderId, woId)
                    .eq(StationLoading::getStationCode, stationCode)
                    .eq(StationLoading::getMaterialId, it.getChildMaterialId())
                    .eq(StationLoading::getStatus, "ACTIVE")
                    .orderByAsc(StationLoading::getId));
            for (StationLoading l : ledger) {
                if (need <= 0) break;
                int avail = n(l.getRemainQty());
                if (avail <= 0) continue;
                int take = Math.min(avail, need);
                l.setRemainQty(avail - take);
                loadingLedgerMapper.updateById(l);
                MdMaterialBatch b = batchMapper.selectOne(new LambdaQueryWrapper<MdMaterialBatch>()
                        .eq(MdMaterialBatch::getBatchNo, l.getBatchNo()));
                if (b != null) {
                    b.setConsumedQty(n(b.getConsumedQty()) + take);
                    b.setRemainQty(n(b.getRemainQty()) - take);
                    batchMapper.updateById(b);
                }
                need -= take;
            }
        }
    }

    /* ==================== 内部 ==================== */

    private MdStation loadStation(String stationCode) {
        MdStation st = stationMapper.selectOne(new LambdaQueryWrapper<MdStation>()
                .eq(MdStation::getStationCode, stationCode));
        if (st == null) throw new BizException("工位不存在: " + stationCode);
        return st;
    }

    private List<StationLoading> activeLedger(Long woId, String stationCode, Long materialId) {
        return loadingLedgerMapper.selectList(new LambdaQueryWrapper<StationLoading>()
                .eq(StationLoading::getWorkOrderId, woId)
                .eq(StationLoading::getStationCode, stationCode)
                .eq(StationLoading::getMaterialId, materialId)
                .eq(StationLoading::getStatus, "ACTIVE"));
    }

    private int n(Integer v) { return v == null ? 0 : v; }
}
