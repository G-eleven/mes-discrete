package com.tws.mes.execution.event;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 过站完成事件（应用内事件，学习版用 Spring 事件替位 MQ；
 * 升级 MQ 时发布方/消费方代码结构不变，只换传输层 —— 对应"事件异步走 MQ 削峰"的叙事）。
 */
@Data
@AllArgsConstructor
public class StationCheckinEvent {

    private Long workOrderId;
    private String sn;
    private String snType;
    private Integer seq;
    /** OK / NG */
    private String result;
    /** 是否该 SN 类型的最后一道工序（OK 即完工） */
    private boolean lastOp;
}
