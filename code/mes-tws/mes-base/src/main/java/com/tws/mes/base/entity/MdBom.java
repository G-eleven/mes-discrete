package com.tws.mes.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** BOM 主表 */
@Data
@TableName("md_bom")
public class MdBom {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String bomCode;
    private Long productMaterialId;
    private String version;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
