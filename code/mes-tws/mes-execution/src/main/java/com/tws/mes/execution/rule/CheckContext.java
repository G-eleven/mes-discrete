package com.tws.mes.execution.rule;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.tws.mes.base.entity.MdStation;
import com.tws.mes.execution.dto.CheckinDTO;
import com.tws.mes.execution.entity.PlanWorkOrder;
import com.tws.mes.execution.entity.PlanWoOperation;
import com.tws.mes.execution.entity.SnRegistry;
import lombok.Data;

/**
 * 过站校验上下文：一次过站请求的全部相关信息。
 * 由 CheckinService 在规则链执行前一次性装配，规则只读不查库（除了个别规则自持 mapper），
 * 保证"装配一次、链式校验"，也是面试里讲"责任链模式"的落点。
 */
@Data
public class CheckContext {

    private CheckinDTO dto;
    /** 工位（扫码进来先换算成工位 -> 工序） */
    private MdStation station;
    /** SN 档案 */
    private SnRegistry sn;
    /** SN 所属工单 */
    private PlanWorkOrder wo;
    /** 工单工序快照（目标工序） */
    private PlanWoOperation woOp;
    /** 快照防呆规则解析结果（空规则 → 空 JSON 对象） */
    private JSONObject rules;
    /** 本次过站轮次：0 首过；维修回流后 1..n */
    private int retestRound;

    public String ruleSnType() {
        return rules == null ? "MACHINE" : rules.getStr("snType", "MACHINE");
    }

    public static JSONObject parseRules(String checkRules) {
        if (checkRules == null || checkRules.trim().isEmpty()) {
            return new JSONObject();
        }
        return JSONUtil.parseObj(checkRules);
    }
}
