package com.tws.mes.execution.rule.rules;

import com.tws.mes.common.constant.MesConst;
import com.tws.mes.execution.rule.CheckContext;
import com.tws.mes.execution.rule.CheckRule;
import org.springframework.stereotype.Component;

/** 规则1：工单必须处于"生产中"（未开工/暂停/完工的工单不允许过站） */
@Component
public class WoStatusRule implements CheckRule {

    @Override
    public int order() { return 10; }

    @Override
    public String name() { return "工单状态校验"; }

    @Override
    public void check(CheckContext ctx) {
        if (!MesConst.WO_IN_PROGRESS.equals(ctx.getWo().getStatus())) {
            throw new com.tws.mes.common.exception.BizException(String.format(
                    "工单 %s 状态为 %s，只有[生产中]的工单允许过站", ctx.getWo().getWoNo(), ctx.getWo().getStatus()));
        }
    }
}
