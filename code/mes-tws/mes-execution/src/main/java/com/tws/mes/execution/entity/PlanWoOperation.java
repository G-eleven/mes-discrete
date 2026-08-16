package com.tws.mes.execution.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 工单工序快照：工单创建时从 md_routing_operation 复制一份。
 * 之后工艺路线升版/修改不影响本表 —— 保证历史工单"当时怎么做的"可追溯。
 */
@Data
@TableName("plan_wo_operation")
public class PlanWoOperation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workOrderId;
    private Integer seq;
    private String operationCode;
    private String operationName;
    private String operationType;
    private String checkRules;
}
