package com.tws.mes.execution.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 维修记录（维修 OK → SN 置 RETEST 回流重测；NG → 报废 SCRAP） */
@Data
@TableName("repair_record")
public class RepairRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long defectId;
    private String sn;
    private String action;
    /** 根因（如：左耳PCBA问题批次贴装问题麦克风） */
    private String rootCause;
    /** 换料批次（若有） */
    private String changeBatchNo;
    /** OK 修好 / NG 报废 */
    private String result;
    private String repairer;
    private LocalDateTime createTime;
}
