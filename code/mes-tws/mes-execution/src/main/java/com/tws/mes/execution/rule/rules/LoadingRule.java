package com.tws.mes.execution.rule.rules;

import com.tws.mes.common.exception.BizException;
import com.tws.mes.execution.rule.CheckContext;
import com.tws.mes.execution.rule.CheckRule;
import com.tws.mes.execution.service.LoadingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 规则7：上料齐套校验。
 *
 * 升级前：只 count 该工位是否有 LOADING 流水记录（"上没上过料"），不关心上的是什么料、够不够。
 * 升级后：按 MBOM 投料清单逐项校验 —— 该工位应上的每种物料，在 station_loading 台账里
 *         都必须有 ACTIVE 记录，缺任一种即拦截并报缺料明细。
 *
 * 这样"批次-工位-物料-时间"可追溯（反向追溯定位用了哪个供应商批次），
 * 且操作员能从异常信息直接知道"还缺什么料"。
 */
@Component
@RequiredArgsConstructor
public class LoadingRule implements CheckRule {

    private final LoadingService loadingService;

    @Override
    public int order() { return 70; }

    @Override
    public String name() { return "上料齐套校验"; }

    @Override
    public void check(CheckContext ctx) {
        if (!ctx.getRules().getBool("requireLoading", false)) {
            return;
        }
        // 工单未关联 BOM 时无法做 MBOM 齐套校验，跳过（学习版工单均有关联）
        if (ctx.getWo().getBomId() == null) {
            return;
        }
        List<String> shortList = loadingService.shortMaterials(
                ctx.getWo().getId(),
                ctx.getStation().getStationCode(),
                ctx.getStation().getOperationCode(),
                ctx.getWo().getBomId());
        if (!shortList.isEmpty()) {
            throw new BizException(String.format(
                    "工位 %s 上料不齐套，缺料：%s（请先到「工位上料看板」完成上料）",
                    ctx.getStation().getStationCode(), String.join("、", shortList)));
        }
    }
}
