package com.tws.mes.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 物料主数据 */
@Data
@TableName("md_material")
public class MdMaterial {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String materialCode;
    private String materialName;
    /** PRODUCT / SEMI / KEY / RAW / PACK */
    private String materialType;
    private String unit;
    private String spec;
    /** 是否批次管理 */
    private Integer batchManaged;
    private String supplier;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
