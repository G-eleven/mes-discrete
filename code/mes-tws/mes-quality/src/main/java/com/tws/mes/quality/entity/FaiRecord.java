package com.tws.mes.quality.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 首件检验记录（换线/换批次首件确认） */
@Data
@TableName("fai_record")
public class FaiRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workOrderId;
    private String operationCode;
    private String sn;
    /** PASS / FAIL */
    private String result;
    private String checker;
    private String remark;
    private LocalDateTime createTime;
}
