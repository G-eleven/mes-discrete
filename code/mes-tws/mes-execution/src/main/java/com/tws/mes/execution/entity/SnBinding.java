package com.tws.mes.execution.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** SN 绑定关系（多级：左/右耳+盒 -> 整机；uk(child_sn, bind_type) 防重复绑定） */
@Data
@TableName("sn_binding")
public class SnBinding {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String parentSn;
    private String childSn;
    /** LEFT / RIGHT / CASE / BOX / CARTON */
    private String bindType;
    private Long workOrderId;
    private String stationCode;
    private String operator;
    private LocalDateTime createTime;
}
