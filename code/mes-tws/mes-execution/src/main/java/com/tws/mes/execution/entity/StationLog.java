package com.tws.mes.execution.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 过站流水（学习版单表；生产按月分表，在线 12 个月、归档 3 年）。
 * record_type: CHECKIN 过站 | LOADING 上料 | BINDING 绑定
 * checkin_key: 仅 CHECKIN 行填充 "sn:station:轮次"，唯一索引兜底重复过站
 */
@Data
@TableName("station_log")
public class StationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String sn;
    private Long workOrderId;
    private String stationCode;
    private String operationCode;
    private Integer seq;
    private String recordType;
    /** OK / NG（LOADING/BINDING 为 null） */
    private String result;
    private String ngCode;
    /** 测试数据 / 绑定明细 JSON 字符串 */
    private String testData;
    /** 上料批次（LOADING） */
    private String batchNo;
    /** 维修回流轮次：0 首过，1..n 重测 */
    private Integer retestRound;
    private String checkinKey;
    private String operator;
    private LocalDateTime createTime;
}
