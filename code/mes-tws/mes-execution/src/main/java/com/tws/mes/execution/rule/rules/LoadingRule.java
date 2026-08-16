package com.tws.mes.execution.rule.rules;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tws.mes.common.constant.MesConst;
import com.tws.mes.common.exception.BizException;
import com.tws.mes.execution.entity.StationLog;
import com.tws.mes.execution.mapper.StationLogMapper;
import com.tws.mes.execution.rule.CheckContext;
import com.tws.mes.execution.rule.CheckRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 规则7：上料批次校验。
 * 规则声明 requireLoading=true 的工位（组装类），必须先扫批次上料才能生产，
 * 保证"批次-工位-时间"可追溯（反向追溯时定位用了哪个供应商批次）。
 */
@Component
@RequiredArgsConstructor
public class LoadingRule implements CheckRule {

    private final StationLogMapper logMapper;

    @Override
    public int order() { return 70; }

    @Override
    public String name() { return "上料批次校验"; }

    @Override
    public void check(CheckContext ctx) {
        if (!ctx.getRules().getBool("requireLoading", false)) {
            return;
        }
        Long cnt = logMapper.selectCount(new LambdaQueryWrapper<StationLog>()
                .eq(StationLog::getStationCode, ctx.getStation().getStationCode())
                .eq(StationLog::getRecordType, MesConst.RT_LOADING));
        if (cnt == null || cnt == 0) {
            throw new BizException(String.format("工位 %s 尚未上料，请先扫描物料批次完成上料",
                    ctx.getStation().getStationCode()));
        }
    }
}
