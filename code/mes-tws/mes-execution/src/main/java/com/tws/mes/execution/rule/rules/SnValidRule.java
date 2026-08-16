package com.tws.mes.execution.rule.rules;

import com.tws.mes.common.constant.MesConst;
import com.tws.mes.common.exception.BizException;
import com.tws.mes.execution.rule.CheckContext;
import com.tws.mes.execution.rule.CheckRule;
import org.springframework.stereotype.Component;

/**
 * 规则2：SN 合法性——状态与类型。
 * 报废/已完工的 SN 不能再过站；工位工序规则声明的 snType 必须与 SN 类型一致
 * （左耳站扫到右耳 SN、整机 SN 扫到部件站都会被拦）。
 */
@Component
public class SnValidRule implements CheckRule {

    @Override
    public int order() { return 20; }

    @Override
    public String name() { return "SN 合法性校验"; }

    @Override
    public void check(CheckContext ctx) {
        String st = ctx.getSn().getStatus();
        if (MesConst.SN_SCRAP.equals(st)) {
            throw new BizException("SN " + ctx.getSn().getSn() + " 已报废，禁止过站");
        }
        if (MesConst.SN_DONE.equals(st)) {
            throw new BizException("SN " + ctx.getSn().getSn() + " 已完工入库，禁止再次过站");
        }
        if (!ctx.ruleSnType().equals(ctx.getSn().getSnType())) {
            throw new BizException(String.format("工位 %s 加工 [%s] 类型 SN，%s 的类型是 [%s]，请走对应工位",
                    ctx.getStation().getStationCode(), ctx.ruleSnType(), ctx.getSn().getSn(), ctx.getSn().getSnType()));
        }
    }
}
