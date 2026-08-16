package com.tws.mes.execution.rule.rules;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tws.mes.common.constant.MesConst;
import com.tws.mes.common.exception.BizException;
import com.tws.mes.execution.entity.DefectRecord;
import com.tws.mes.execution.mapper.DefectRecordMapper;
import com.tws.mes.execution.rule.CheckContext;
import com.tws.mes.execution.rule.CheckRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 规则3：前置不良闭环校验。
 * SN 存在未闭环不良单（OPEN）时拦截：必须先维修（OK→RETEST 回流重测 / NG→SCRAP 报废），
 * 杜绝"带病流转"——漏检机器流到包装段的质量事故防线。
 */
@Component
@RequiredArgsConstructor
public class OpenDefectRule implements CheckRule {

    private final DefectRecordMapper defectMapper;

    @Override
    public int order() { return 30; }

    @Override
    public String name() { return "前置不良闭环校验"; }

    @Override
    public void check(CheckContext ctx) {
        Long openCnt = defectMapper.selectCount(new LambdaQueryWrapper<DefectRecord>()
                .eq(DefectRecord::getSn, ctx.getSn().getSn())
                .eq(DefectRecord::getStatus, MesConst.DEFECT_OPEN));
        if (openCnt != null && openCnt > 0) {
            throw new BizException(String.format("SN %s 存在 %d 条未闭环不良单，请先完成维修处理", ctx.getSn().getSn(), openCnt));
        }
        if (MesConst.SN_NG.equals(ctx.getSn().getStatus())) {
            throw new BizException("SN 状态为 NG，请先维修闭环后回流重测");
        }
    }
}
