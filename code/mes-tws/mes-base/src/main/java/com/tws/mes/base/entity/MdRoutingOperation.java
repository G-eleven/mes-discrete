package com.tws.mes.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 工艺路线工序明细（check_rules 为 JSON 字符串，保存前做格式校验） */
@Data
@TableName("md_routing_operation")
public class MdRoutingOperation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long routingId;
    private Integer seq;
    private String operationCode;
    /** 防呆规则 JSON：{"snType":"LEFT","requirePrev":true,"testItems":[...],"needBinding":[...],"requireLoading":true} */
    private String checkRules;
}
