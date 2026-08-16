package com.tws.mes.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 工序定义（全厂工序字典） */
@Data
@TableName("md_operation")
public class MdOperation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String operationCode;
    private String operationName;
    /** NORMAL / TEST / BIND / AGING / PACK / IQC */
    private String operationType;
    private Integer status;
    private LocalDateTime createTime;
}
