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
    /** 已消耗量（过站按 BOM 定额扣减累计；学习版预留，暂不自动扣） */
    private Integer consumedQty;
    /** 剩余量（= quantity - consumedQty，上料防错与低量预警的依据） */
    private Integer remainQty;
    /** 1 可用 / 0 冻结 */
    private Integer status;
    private LocalDateTime createTime;
}
