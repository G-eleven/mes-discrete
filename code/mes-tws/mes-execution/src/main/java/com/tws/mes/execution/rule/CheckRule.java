package com.tws.mes.execution.rule;

/**
 * 防呆校验规则（责任链）。
 * 每条规则是一个 Spring Bean，CheckinService 按 order 升序执行；
 * 任何一条抛 BizException 即拦截过站。
 *
 * 想加新规则（练习点）：实现本接口 → @Component → 重启即生效，主流程零改动。
 */
public interface CheckRule {

    /** 执行顺序（小在前） */
    int order();

    /** 规则名（拦截时用户看得懂） */
    String name();

    void check(CheckContext ctx);
}
