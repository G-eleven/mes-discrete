package com.tws.mes.execution.rule.rules;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tws.mes.common.exception.BizException;
import com.tws.mes.execution.entity.PlanWoOperation;
import com.tws.mes.execution.mapper.PlanWoOperationMapper;
import com.tws.mes.execution.rule.CheckContext;
import com.tws.mes.execution.rule.CheckRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 规则4：工序顺序校验（防跳站/漏站）。
 * 取该工单快照中"与 SN 同类型"的工序列表（按 seq 升序），
 * 要求目标工序之前所有同类型工序都已完成（currentSeq >= 其 seq），且目标工序本身未完成。
 * 部件（左/右耳/盒）与整机各自维护进度，互不干扰。
 */
@Component
@RequiredArgsConstructor
public class SeqRule implements CheckRule {

    private final PlanWoOperationMapper woOpMapper;

    @Override
    public int order() { return 40; }

    @Override
    public String name() { return "工序顺序校验"; }

    @Override
    public void check(CheckContext ctx) {
        List<PlanWoOperation> sameTypeOps = woOpMapper.selectList(
                        new LambdaQueryWrapper<PlanWoOperation>()
                                .eq(PlanWoOperation::getWorkOrderId, ctx.getWo().getId()))
                .stream()
                .filter(op -> CheckContext.parseRules(op.getCheckRules()).getStr("snType", "MACHINE")
                        .equals(ctx.getSn().getSnType()))
                .sorted(Comparator.comparing(PlanWoOperation::getSeq))
                .collect(Collectors.toList());

        int targetSeq = ctx.getWoOp().getSeq();
        int currentSeq = ctx.getSn().getCurrentSeq() == null ? 0 : ctx.getSn().getCurrentSeq();

        // 目标工序之前的所有同类型工序必须已完成
        List<PlanWoOperation> unfinished = sameTypeOps.stream()
                .filter(op -> op.getSeq() < targetSeq && op.getSeq() > currentSeq)
                .collect(Collectors.toList());
        if (!unfinished.isEmpty()) {
            throw new BizException(String.format("跳站/漏站拦截：SN %s 还有未完成的前置工序 %s（当前进度 seq=%d）",
                    ctx.getSn().getSn(),
                    unfinished.stream().map(op -> op.getSeq() + ":" + op.getOperationName()).collect(Collectors.joining("、")),
                    currentSeq));
        }
        // 目标本站已完成（同轮次下 currentSeq >= 目标 seq 说明已通过；重测轮次由 DuplicateRule 放行）
        if (currentSeq >= targetSeq) {
            throw new BizException(String.format("SN %s 已通过工序[%s]，请勿重复流转（维修重测由系统按轮次放行）",
                    ctx.getSn().getSn(), ctx.getWoOp().getOperationName()));
        }
    }
}
