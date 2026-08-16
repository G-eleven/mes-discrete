package com.tws.mes.execution.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SN 注册表 —— "一机多码"的载体。
 * 整机(MACHINE)/左耳(LEFT)/右耳(RIGHT)/盒(CASE)…都是一行记录；
 * 部件 SN 带 batch_no（来料批次），是反向追溯的锚点。
 */
@Data
@TableName("sn_registry")
public class SnRegistry {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String sn;
    /** MACHINE / LEFT / RIGHT / CASE / BOX / CARTON */
    private String snType;
    private Long workOrderId;
    private String batchNo;
    /** 绑定后回填的父 SN（如左耳 -> 整机） */
    private String parentSn;
    /** 已通过的最大工序顺序（NG 不推进，维修重测通过后才推进） */
    private Integer currentSeq;
    /** INIT / IN_LINE / NG / RETEST / DONE / SCRAP */
    private String status;
    private String firmwareVersion;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
