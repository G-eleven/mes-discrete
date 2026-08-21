package com.tws.mes.execution.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工位上料台账：一次上料动作 = 一行。
 * 记录"哪个工单 / 哪个工位 / 上了哪个物料 / 哪个批次 / 上料量 / 剩余量"，
 * 是「工位上料看板」与 LoadingRule 齐套校验的数据源。
 * status: ACTIVE 在用 / UNLOADED 已退料
 */
@Data
@TableName("station_loading")
public class StationLoading {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workOrderId;
    private String stationCode;
    private String operationCode;
    private Long materialId;
    private String batchNo;
    /** 上料量 */
    private Integer loadingQty;
    /** 剩余量（初始 = loadingQty；过站按 BOM 定额扣减递减，学习版暂留字段） */
    private Integer remainQty;
    /** ACTIVE 在用 / UNLOADED 已退料 */
    private String status;
    private String operator;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
