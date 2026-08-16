package com.tws.mes.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 产线 */
@Data
@TableName("md_line")
public class MdLine {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String lineCode;
    private String lineName;
    private String workshop;
    private Integer status;
    private LocalDateTime createTime;
}
