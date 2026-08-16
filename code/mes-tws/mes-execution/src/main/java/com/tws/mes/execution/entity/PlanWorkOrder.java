package com.tws.mes.execution.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 生产工单。
 * version 字段配合 MyBatis-Plus @Version 做乐观锁：
 * UPDATE 时自动带 WHERE version=? 且 version+1，
 * 两个计划员同时点"下达"，后提交的那个会更新 0 行 → 提示刷新重试。
 */
@Data
@TableName("plan_work_order")
public class PlanWorkOrder {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String woNo;
    private Long productMaterialId;
    private Long bomId;
    private Long routingId;
    private Integer routingVersion;
    private Integer planQty;
    private Integer okQty;
    private Integer ngQty;
    private Integer snGenerated;
    /** CREATED/RELEASED/IN_PROGRESS/PAUSED/COMPLETED/CLOSED */
    private String status;
    @Version
    private Integer version;
    private LocalDate planStartDate;
    private LocalDate planEndDate;
    private String createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
