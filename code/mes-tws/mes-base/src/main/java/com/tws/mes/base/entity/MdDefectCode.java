package com.tws.mes.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 不良代码字典 */
@Data
@TableName("md_defect_code")
public class MdDefectCode {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String defectCode;
    private String defectName;
    /** APPEARANCE / ACOUSTIC / FUNC / OTHER */
    private String category;
    private Integer status;
    private LocalDateTime createTime;
}
