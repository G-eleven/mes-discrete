package com.tws.mes.execution.vo;

import com.tws.mes.execution.entity.PlanWorkOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 工单列表 VO：关联字段（物料编码/名称、路线名）由分页 SQL 填充 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderVO extends PlanWorkOrder {

    private String productMaterialCode;
    private String productMaterialName;
    private String routingName;
}
