package com.tws.mes.execution.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 不良记录。过站 NG 时由过站引擎自动开单（OPEN）。
 * 放在 execution 模块的原因：过站拦截规则需要查"未闭环不良"，且 NG 开单是执行域动作；
 * 质量域（维修/统计）通过模块依赖消费本表。
 */
@Data
@TableName("defect_record")
public class DefectRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String sn;
    private Long workOrderId;
    private String stationCode;
    private String operationCode;
    private String defectCode;
    private String defectDesc;
    /** CHECKIN 过站 / RECHECK 复检 / AUDIT 抽检 */
    private String discoverType;
    /** 第几轮维修（重测轮次 = repair_round + 1） */
    private Integer repairRound;
    /** OPEN 待修 / REPAIRED 已修复 / SCRAP 报废 */
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
