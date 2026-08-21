package com.tws.mes.execution.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tws.mes.base.entity.MdStation;
import com.tws.mes.base.mapper.MdStationMapper;
import com.tws.mes.base.service.CurrentUserService;
import com.tws.mes.common.constant.MesConst;
import com.tws.mes.common.exception.BizException;
import com.tws.mes.common.lock.RedisLockService;
import com.tws.mes.execution.dto.CheckinDTO;
import com.tws.mes.execution.entity.*;
import com.tws.mes.execution.event.StationCheckinEvent;
import com.tws.mes.execution.mapper.*;
import com.tws.mes.execution.rule.CheckContext;
import com.tws.mes.execution.rule.CheckRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 过站引擎 —— 本项目的心脏。
 *
 * 主流程：工位换算 → 装配校验上下文 → Redis 锁（防并发双击）→ 八大防呆规则链
 *        → 同步落流水 + 更新 SN 状态（NG 自动开不良单）→ 发布异步事件（工单进度）。
 *
 * 三道防线防重复过站：Redis SET NX 锁 → 规则链 DuplicateRule → checkin_key 唯一索引。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckinService {

    private final MdStationMapper stationMapper;
    private final SnRegistryMapper snMapper;
    private final PlanWorkOrderMapper woMapper;
    private final PlanWoOperationMapper woOpMapper;
    private final StationLogMapper logMapper;
    private final DefectRecordMapper defectMapper;
    private final RedisLockService lockService;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;
    /** Spring 注入所有 CheckRule 实现，按 order 排序成链 */
    private final List<CheckRule> rules;
    private final LoadingService loadingService;

    @Transactional
    public java.util.Map<String, Object> checkin(CheckinDTO dto) {
        if (StrUtil.isBlank(dto.getSn()) || StrUtil.isBlank(dto.getStationCode())) {
            throw new BizException("SN 与工位码不能为空");
        }
        // 1. 工位换算
        MdStation station = stationMapper.selectOne(new LambdaQueryWrapper<MdStation>()
                .eq(MdStation::getStationCode, dto.getStationCode()));
        if (station == null || station.getStatus() != 1) {
            throw new BizException("工位不存在或未启用: " + dto.getStationCode());
        }
        // 2. 装配上下文
        SnRegistry sn = snMapper.selectOne(new LambdaQueryWrapper<SnRegistry>().eq(SnRegistry::getSn, dto.getSn()));
        if (sn == null) {
            throw new BizException("SN 未注册: " + dto.getSn() + "（请先在 SN 管理生成/注册）");
        }
        if (sn.getWorkOrderId() == null) {
            throw new BizException("SN 未关联工单，部件请先过自身工序");
        }
        PlanWorkOrder wo = woMapper.selectById(sn.getWorkOrderId());
        if (wo == null) {
            throw new BizException("SN 所属工单不存在");
        }
        PlanWoOperation woOp = woOpMapper.selectOne(new LambdaQueryWrapper<PlanWoOperation>()
                .eq(PlanWoOperation::getWorkOrderId, wo.getId())
                .eq(PlanWoOperation::getOperationCode, station.getOperationCode()));
        if (woOp == null) {
            throw new BizException(String.format("工单 %s 的工序快照中没有工序 %s（该工位不属于这张工单）",
                    wo.getWoNo(), station.getOperationCode()));
        }
        CheckContext ctx = new CheckContext();
        ctx.setDto(dto);
        ctx.setStation(station);
        ctx.setSn(sn);
        ctx.setWo(wo);
        ctx.setWoOp(woOp);
        ctx.setRules(CheckContext.parseRules(woOp.getCheckRules()));
        ctx.setRetestRound(MesConst.SN_RETEST.equals(sn.getStatus()) ? nextRetestRound(sn.getSn()) : 0);

        // 3. Redis 锁：同 SN 同工位 5 秒内只允许一次提交（防扫码枪双击/网络重放）
        String lockKey = "checkin:" + dto.getSn() + ":" + dto.getStationCode();
        String lockToken = lockService.tryLock(lockKey, 5000);
        if (lockToken == null) {
            throw new BizException("该 SN 正在工位 " + dto.getStationCode() + " 处理中，请勿重复扫码");
        }
        try {
            // 4. 防呆规则链（order 升序，任一失败即拦截）
            List<CheckRule> chain = rules.stream().sorted(Comparator.comparingInt(CheckRule::order))
                    .collect(Collectors.toList());
            for (CheckRule rule : chain) {
                rule.check(ctx);
            }
            // 5. 落流水 + 状态更新
            String operator = currentUserService.currentUsername();
            StationLog logRow = new StationLog();
            logRow.setSn(sn.getSn());
            logRow.setWorkOrderId(wo.getId());
            logRow.setStationCode(station.getStationCode());
            logRow.setOperationCode(woOp.getOperationCode());
            logRow.setSeq(woOp.getSeq());
            logRow.setRecordType(MesConst.RT_CHECKIN);
            logRow.setResult(dto.getResult());
            logRow.setNgCode(dto.getNgCode());
            logRow.setTestData(dto.getTestData() == null ? null : JSONUtil.toJsonStr(dto.getTestData()));
            logRow.setRetestRound(ctx.getRetestRound());
            logRow.setCheckinKey(sn.getSn() + ":" + station.getStationCode() + ":" + ctx.getRetestRound());
            logRow.setOperator(operator);
            try {
                logMapper.insert(logRow);   // 唯一索引兜底并发
            } catch (DuplicateKeyException e) {
                throw new BizException("重复过站（唯一索引兜底拦截）：该 SN 已在此工位通过本轮次");
            }

            boolean lastOp = isLastOpOfItsType(ctx);
            if ("OK".equals(dto.getResult())) {
                sn.setCurrentSeq(woOp.getSeq());
                sn.setStatus(lastOp ? MesConst.SN_DONE : MesConst.SN_IN_LINE);
                if (dto.getTestData() != null && dto.getTestData().containsKey("firmware")) {
                    sn.setFirmwareVersion(String.valueOf(dto.getTestData().get("firmware")));
                }
                snMapper.updateById(sn);
                // 按 MBOM 定额扣减本工位上料台账与批次库存（消耗闭环）
                if (wo.getBomId() != null) {
                    loadingService.consume(wo.getId(), station.getStationCode(), woOp.getOperationCode(), wo.getBomId());
                }
            } else {
                // NG：SN 停线，自动开不良单（OPEN），等维修闭环
                sn.setStatus(MesConst.SN_NG);
                snMapper.updateById(sn);
                DefectRecord defect = new DefectRecord();
                defect.setSn(sn.getSn());
                defect.setWorkOrderId(wo.getId());
                defect.setStationCode(station.getStationCode());
                defect.setOperationCode(woOp.getOperationCode());
                defect.setDefectCode(dto.getNgCode());
                defect.setDiscoverType("CHECKIN");
                defect.setRepairRound(ctx.getRetestRound());
                defect.setStatus(MesConst.DEFECT_OPEN);
                defectMapper.insert(defect);
            }
            // 6. 异步事件（工单进度等非关键路径）
            eventPublisher.publishEvent(new StationCheckinEvent(
                    wo.getId(), sn.getSn(), sn.getSnType(), woOp.getSeq(), dto.getResult(), lastOp));

            return java.util.Collections.singletonMap("message",
                    ("OK".equals(dto.getResult()) ? "过站成功 ✔ " : "已判定 NG，生成不良单（SN 停线待修）")
                            + String.format("（%s / %s / %s / 第%d轮）", sn.getSn(), station.getStationCode(),
                            woOp.getOperationName(), ctx.getRetestRound()));
        } finally {
            lockService.unlock(lockKey, lockToken);
        }
    }

    /**
     * 模拟器辅助：给定工单+工位，返回该工序快照的规则要点
     * （前端据此动态渲染测试项输入框，并展示本站加工哪种 SN）。
     */
    public java.util.Map<String, Object> stationContext(Long woId, String stationCode) {
        MdStation station = stationMapper.selectOne(new LambdaQueryWrapper<MdStation>()
                .eq(MdStation::getStationCode, stationCode));
        if (station == null) throw new BizException("工位不存在: " + stationCode);
        PlanWoOperation woOp = woOpMapper.selectOne(new LambdaQueryWrapper<PlanWoOperation>()
                .eq(PlanWoOperation::getWorkOrderId, woId)
                .eq(PlanWoOperation::getOperationCode, station.getOperationCode()));
        if (woOp == null) return java.util.Collections.singletonMap("exists", false);
        CheckContext ctx = new CheckContext();
        ctx.setRules(CheckContext.parseRules(woOp.getCheckRules()));
        java.util.Map<String, Object> r = new java.util.HashMap<>();
        r.put("exists", true);
        r.put("seq", woOp.getSeq());
        r.put("operationName", woOp.getOperationName());
        r.put("operationCode", woOp.getOperationCode());
        r.put("snType", ctx.ruleSnType());
        r.put("testItems", ctx.getRules().getJSONArray("testItems"));
        r.put("needBinding", ctx.getRules().getJSONArray("needBinding"));
        r.put("requireLoading", ctx.getRules().getBool("requireLoading", false));
        return r;
    }

    /** 该 SN 类型的最后一道工序是否就是当前工序 */
    private boolean isLastOpOfItsType(CheckContext ctx) {
        List<PlanWoOperation> sameType = woOpMapper.selectList(new LambdaQueryWrapper<PlanWoOperation>()
                        .eq(PlanWoOperation::getWorkOrderId, ctx.getWo().getId())).stream()
                .filter(op -> CheckContext.parseRules(op.getCheckRules()).getStr("snType", "MACHINE")
                        .equals(ctx.getSn().getSnType()))
                .sorted(Comparator.comparing(PlanWoOperation::getSeq))
                .collect(Collectors.toList());
        return !sameType.isEmpty() && sameType.get(sameType.size() - 1).getSeq().equals(ctx.getWoOp().getSeq());
    }

    private int nextRetestRound(String sn) {
        Integer max = logMapper.selectList(new LambdaQueryWrapper<StationLog>()
                        .eq(StationLog::getSn, sn).eq(StationLog::getRecordType, MesConst.RT_CHECKIN)).stream()
                .map(StationLog::getRetestRound).filter(r -> r != null)
                .max(Integer::compareTo).orElse(0);
        return max + 1;
    }
}
