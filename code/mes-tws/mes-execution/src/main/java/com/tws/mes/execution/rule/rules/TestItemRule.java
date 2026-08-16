package com.tws.mes.execution.rule.rules;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.tws.mes.common.exception.BizException;
import com.tws.mes.execution.rule.CheckContext;
import com.tws.mes.execution.rule.CheckRule;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 规则8：测试项判定（固件版本/声学参数等）。
 * 规则 testItems 声明判定项：{"key":"firmware","op":"eq","value":"1.2.5"}；
 * result=OK 时逐项比对测试数据，任何一项不符都给出"期望 vs 实际"提示
 * （现场看到提示就会改判 NG，而不是靠人眼比对）。
 */
@Component
public class TestItemRule implements CheckRule {

    @Override
    public int order() { return 80; }

    @Override
    public String name() { return "测试项判定"; }

    @Override
    public void check(CheckContext ctx) {
        JSONArray items = ctx.getRules().getJSONArray("testItems");
        if (items == null || items.isEmpty()) {
            return;
        }
        if (!"OK".equals(ctx.getDto().getResult())) {
            // 判 NG 时不做判定，但必须给不良代码
            if (StrUtil.isBlank(ctx.getDto().getNgCode())) {
                throw new BizException("判定 NG 必须选择不良代码");
            }
            return;
        }
        Map<String, Object> testData = ctx.getDto().getTestData();
        for (Object o : items) {
            JSONObject item = (JSONObject) o;
            String key = item.getStr("key");
            String op = item.getStr("op", "eq");
            Object expected = item.get("value");
            Object actual = testData == null ? null : testData.get(key);
            if (actual == null) {
                throw new BizException(String.format("测试项 [%s] 缺少测试数据，无法判定 OK", key));
            }
            if (!match(String.valueOf(actual), op, String.valueOf(expected))) {
                throw new BizException(String.format("测试项 [%s] 不合格：期望 %s %s，实际 %s —— 请改判 NG 并登记不良",
                        key, op, expected, actual));
            }
        }
    }

    private boolean match(String actual, String op, String expected) {
        if ("eq".equals(op)) return actual.equalsIgnoreCase(expected);
        if ("ne".equals(op)) return !actual.equalsIgnoreCase(expected);
        if (NumberUtil.isNumber(actual) && NumberUtil.isNumber(expected)) {
            double a = Double.parseDouble(actual), e = Double.parseDouble(expected);
            if ("gt".equals(op)) return a > e;
            if ("ge".equals(op)) return a >= e;
            if ("lt".equals(op)) return a < e;
            if ("le".equals(op)) return a <= e;
        }
        throw new BizException("不支持的比较运算: " + op);
    }
}
