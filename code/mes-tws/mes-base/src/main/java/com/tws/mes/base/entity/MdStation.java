package com.tws.mes.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 工位（绑定工序；过站时扫的是工位码） */
@Data
@TableName("md_station")
public class MdStation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String stationCode;
    private String stationName;
    private Long lineId;
    private String operationCode;
    private Integer status;
    private LocalDateTime createTime;
}
