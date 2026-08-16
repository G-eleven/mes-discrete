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
 * 规则5：重复过站校验（库内兜底版）。
 * 同一 SN 在同一工位、同一轮次已 OK 通过则拦截。
 * 并发场景的第一道防线是 Redis 锁（CheckinService 里的 SET NX），
 * 本规则 + checkin_key 唯一索引是二三道防线：锁抖动/重放请求也进不来。
 */
@Component
@RequiredArgsConstructor
public class DuplicateRule implements CheckRule {

    private final StationLogMapper logMapper;

    @Override
    public int order() { return 50; }

    @Override
    public String name() { return "重复过站校验"; }

    @Override
    public void check(CheckContext ctx) {
        Long cnt = logMapper.selectCount(new LambdaQueryWrapper<StationLog>()
                .eq(StationLog::getSn, ctx.getSn().getSn())
                .eq(StationLog::getStationCode, ctx.getStation().getStationCode())
                .eq(StationLog::getRecordType, MesConst.RT_CHECKIN)
                .eq(StationLog::getRetestRound, ctx.getRetestRound())
                .eq(StationLog::getResult, "OK"));
        if (cnt != null && cnt > 0) {
            throw new BizException(String.format("重复过站：SN %s 已在工位 %s 通过（第 %d 轮）",
                    ctx.getSn().getSn(), ctx.getStation().getStationCode(), ctx.getRetestRound()));
        }
    }
}
