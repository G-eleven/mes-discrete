package com.tws.mes.execution.rule.rules;

import cn.hutool.json.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tws.mes.common.exception.BizException;
import com.tws.mes.execution.entity.SnBinding;
import com.tws.mes.execution.mapper.SnBindingMapper;
import com.tws.mes.execution.rule.CheckContext;
import com.tws.mes.execution.rule.CheckRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则6：绑定完整性校验。
 * 三码绑定站（及之后所有整机站）要求 LEFT/RIGHT/CASE 已绑到整机 SN，
 * 防止"空盒出厂"——只包装了盒没装耳机的经典质量事故。
 */
@Component
@RequiredArgsConstructor
public class BindingRule implements CheckRule {

    private final SnBindingMapper bindingMapper;

    @Override
    public int order() { return 60; }

    @Override
    public String name() { return "绑定完整性校验"; }

    @Override
    public void check(CheckContext ctx) {
        JSONArray need = ctx.getRules().getJSONArray("needBinding");
        if (need == null || need.isEmpty()) {
            return;
        }
        List<String> bound = bindingMapper.selectList(new LambdaQueryWrapper<SnBinding>()
                        .eq(SnBinding::getParentSn, ctx.getSn().getSn()))
                .stream().map(SnBinding::getBindType).collect(java.util.stream.Collectors.toList());
        List<String> missing = new ArrayList<>();
        for (Object t : need) {
            if (!bound.contains(String.valueOf(t))) {
                missing.add(String.valueOf(t));
            }
        }
        if (!missing.isEmpty()) {
            throw new BizException(String.format("绑定不完整：整机 %s 缺少子件 %s，请先在三码绑定站完成绑定",
                    ctx.getSn().getSn(), String.join("、", missing)));
        }
    }
}
