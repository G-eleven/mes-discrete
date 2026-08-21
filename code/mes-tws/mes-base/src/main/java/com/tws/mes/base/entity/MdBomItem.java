package com.tws.mes.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** BOM 子件明细 */
@Data
@TableName("md_bom_item")
public class MdBomItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bomId;
    private Long childMaterialId;
    private Double quantity;
    /** 投料工序编码（MBOM：标记该子件在哪道工序上料/消耗），NULL=未指定 */
    private String operationCode;
}
