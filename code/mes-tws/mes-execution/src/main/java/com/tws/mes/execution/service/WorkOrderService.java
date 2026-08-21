package com.tws.mes.execution.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tws.mes.base.entity.MdMaterial;
import com.tws.mes.base.entity.MdOperation;
import com.tws.mes.base.entity.MdRouting;
import com.tws.mes.base.entity.MdRoutingOperation;
import com.tws.mes.base.mapper.MdMaterialMapper;
import com.tws.mes.base.mapper.MdOperationMapper;
import com.tws.mes.base.mapper.MdRoutingMapper;
import com.tws.mes.base.mapper.MdRoutingOperationMapper;
import com.tws.mes.base.service.CurrentUserService;
import com.tws.mes.common.constant.MesConst;
import com.tws.mes.common.exception.BizException;
import com.tws.mes.common.result.PageResult;
import com.tws.mes.execution.entity.PlanWorkOrder;
import com.tws.mes.execution.entity.PlanWoOperation;
import com.tws.mes.execution.mapper.PlanWorkOrderMapper;
import com.tws.mes.execution.mapper.PlanWoOperationMapper;
import com.tws.mes.execution.vo.WorkOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工单管理 —— 状态机 + 工艺路线快照 + 乐观锁，本模块三个核心设计点。
 *
 * 状态机：CREATED → RELEASED → IN_PROGRESS ⇄ PAUSED → COMPLETED → CLOSED
 * 快照：创建时复制路线工序（含防呆规则 JSON）到 plan_wo_operation，路线升版不影响在制工单
 * 乐观锁：version 字段防两个计划员并发操作同一工单
 */
@Service
@RequiredArgsConstructor
public class WorkOrderService {

    /** 状态机迁移表：action -> (允许的当前状态, 目标状态)。Java 8 无 Map.of，用静态块初始化 */
    private static final Map<String, String[]> TRANSITIONS = new HashMap<>();

    static {
        TRANSITIONS.put("release",  new String[]{MesConst.WO_CREATED,      MesConst.WO_RELEASED});
        TRANSITIONS.put("start",    new String[]{MesConst.WO_RELEASED,     MesConst.WO_IN_PROGRESS});
        TRANSITIONS.put("pause",    new String[]{MesConst.WO_IN_PROGRESS,  MesConst.WO_PAUSED});
        TRANSITIONS.put("resume",   new String[]{MesConst.WO_PAUSED,       MesConst.WO_IN_PROGRESS});
        TRANSITIONS.put("complete", new String[]{MesConst.WO_IN_PROGRESS,  MesConst.WO_COMPLETED});
        TRANSITIONS.put("close",    new String[]{MesConst.WO_COMPLETED,    MesConst.WO_CLOSED});
    }

    private final PlanWorkOrderMapper woMapper;
    private final PlanWoOperationMapper woOpMapper;
    private final MdMaterialMapper materialMapper;
    private final MdRoutingMapper routingMapper;
    private final MdRoutingOperationMapper routingOpMapper;
    private final MdOperationMapper operationMapper;
    private final CurrentUserService currentUserService;

    public PageResult<WorkOrderVO> page(long page, long size, String keyword, String status) {
        IPage<WorkOrderVO> p = woMapper.pageWo(new Page<>(page, size), keyword, status);
        return PageResult.of(p.getTotal(), p.getRecords());
    }

    public Map<String, Object> detail(Long id) {
        PlanWorkOrder wo = woMapper.selectById(id);
        if (wo == null) throw new BizException("工单不存在");
        List<PlanWoOperation> ops = woOpMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PlanWoOperation>()
                        .eq(PlanWoOperation::getWorkOrderId, id)
                        .orderByAsc(PlanWoOperation::getSeq));
        Map<String, Object> r = new HashMap<>();
        r.put("wo", wo);
        r.put("operations", ops);
        return r;
    }

    /**
     * 创建工单：校验物料/路线 → 生成工单号 → 落库 → 复制路线工序快照。
     */
    @Transactional
    public PlanWorkOrder create(Long productMaterialId, Long bomId, Long routingId,
                                Integer planQty, LocalDate planStart, LocalDate planEnd) {
        MdMaterial product = materialMapper.selectById(productMaterialId);
        if (product == null || !"PRODUCT".equals(product.getMaterialType()))
            throw new BizException("请选择成品物料");
        if (planQty == null || planQty <= 0) throw new BizException("计划数量必须大于 0");

        MdRouting routing = routingMapper.selectById(routingId);
        if (routing == null) throw new BizException("工艺路线不存在");
        if (routing.getStatus() != 2) throw new BizException("只能引用[已发布]的工艺路线版本");
        if (!routing.getProductMaterialId().equals(productMaterialId))
            throw new BizException("工艺路线与成品物料不匹配");

        PlanWorkOrder wo = new PlanWorkOrder();
        wo.setWoNo(nextWoNo()); // 生成单号
        wo.setProductMaterialId(productMaterialId);
        wo.setBomId(bomId);
        wo.setRoutingId(routingId);
        wo.setRoutingVersion(routing.getVersion()); // 把路线「当时的版本号」固化到工单上
        wo.setPlanQty(planQty);
        wo.setOkQty(0);
        wo.setNgQty(0);
        wo.setSnGenerated(0);
        wo.setStatus(MesConst.WO_CREATED);
        wo.setVersion(0); // 乐观锁初始版本 = 0
        wo.setPlanStartDate(planStart);
        wo.setPlanEndDate(planEnd);
        wo.setCreateBy(currentUserService.currentUsername());
        woMapper.insert(wo); // ← 写第一张表 plan_work_order

        // 工艺路线快照（核心设计：工单与路线版本解耦）
        List<MdRoutingOperation> routingOps = routingOpMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MdRoutingOperation>()
                        .eq(MdRoutingOperation::getRoutingId, routingId)
                        .orderByAsc(MdRoutingOperation::getSeq)); // 查出这条路线的所有工序
        Map<String, MdOperation> opDict = operationMapper.selectList(null).stream()
                .collect(Collectors.toMap(MdOperation::getOperationCode, o -> o, (a, b) -> a)); // 工序字典
        for (MdRoutingOperation rop : routingOps) {
            PlanWoOperation snap = new PlanWoOperation();
            snap.setWorkOrderId(wo.getId()); // 把工序绑到刚建的工单
            snap.setSeq(rop.getSeq());
            snap.setOperationCode(rop.getOperationCode());
            MdOperation dict = opDict.get(rop.getOperationCode());
            snap.setOperationName(dict == null ? rop.getOperationCode() : dict.getOperationName());
            snap.setOperationType(dict == null ? "NORMAL" : dict.getOperationType());
            snap.setCheckRules(rop.getCheckRules()); // 把防呆规则 JSON 一起拷过来
            woOpMapper.insert(snap); // ← 写第二张表 plan_wo_operation（N 行）
        }
        return wo;
    }

    /** 状态流转：先校验迁移合法性，再依赖 @Version 乐观锁提交 */
    public void transition(Long id, String action) {
        String[] rule = TRANSITIONS.get(action);
        if (rule == null) throw new BizException("不支持的操作: " + action);
        PlanWorkOrder wo = woMapper.selectById(id);
        if (wo == null) throw new BizException("工单不存在");
        if (!rule[0].equals(wo.getStatus())) {
            throw new BizException(String.format("工单[%s]当前状态为 %s，不能执行[%s]",
                    wo.getWoNo(), wo.getStatus(), actionName(action)));
        }
        wo.setStatus(rule[1]);
        int rows = woMapper.updateById(wo);   // @Version 自动带 version 条件
        if (rows == 0) {
            throw new BizException("工单状态已被他人变更，请刷新后重试（乐观锁生效）");
        }
    }

    private String actionName(String action) {
        Map<String, String> names = new HashMap<>();
        names.put("release", "下达"); names.put("start", "开工"); names.put("pause", "暂停");
        names.put("resume", "恢复"); names.put("complete", "完工"); names.put("close", "关闭");
        return names.containsKey(action) ? names.get(action) : action;
    }

    /** 工单号：WO + yyyyMMdd + 3位序号。取当日"最大序号+1"而非计数（序号不连续时计数会撞号）。
     *  并发下极端情况仍可能重号，生产环境用 Redis INCR 保证；学习版由 wo_no 唯一索引兜底。 */
    private String nextWoNo() {
        String prefix = "WO" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        List<PlanWorkOrder> todays = woMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PlanWorkOrder>()
                        .likeRight(PlanWorkOrder::getWoNo, prefix));
        int maxSeq = todays.stream()
                .map(w -> w.getWoNo().substring(prefix.length()))
                .filter(s -> s.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max().orElse(0);
        return prefix + String.format("%03d", maxSeq + 1);
    }

    /** 供其他模块（质量/追溯/看板）按名称搜索 */
    public PlanWorkOrder getByNo(String woNo) {
        if (StrUtil.isBlank(woNo)) return null;
        return woMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PlanWorkOrder>()
                        .eq(PlanWorkOrder::getWoNo, woNo));
    }
}
