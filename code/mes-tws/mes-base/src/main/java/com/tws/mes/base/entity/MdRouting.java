package com.tws.mes.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 工艺路线（版本化：routing_code + version 唯一） */
@Data
@TableName("md_routing")
public class MdRouting {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String routingCode;
    private String routingName;
    private Long productMaterialId;
    private Integer version;
    /** 0 停用 / 1 草稿 / 2 已发布（工单只能引用已发布版本） */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
