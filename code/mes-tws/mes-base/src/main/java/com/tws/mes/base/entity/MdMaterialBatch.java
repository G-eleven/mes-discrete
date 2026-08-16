package com.tws.mes.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 物料批次（关键料/半成品来料批次，上料与反向追溯的锚点） */
@Data
@TableName("md_material_batch")
public class MdMaterialBatch {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long materialId;
    /** 批次号全厂唯一 */
    private String batchNo;
    private String supplier;
    private LocalDateTime arriveTime;
    private Integer quantity;
    /** 1 可用 / 0 冻结 */
    private Integer status;
    private LocalDateTime createTime;
}
