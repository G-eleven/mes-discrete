package com.tws.mes.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 追溯任务留痕：每次查询记录口径与耗时（对齐叙事"审厂追溯报告 2 小时 → 秒级"） */
@Data
@TableName("trace_task")
public class TraceTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskNo;
    /** FORWARD / REVERSE */
    private String traceType;
    /** SN 或批次号 */
    private String queryKey;
    private Integer resultCount;
    private Integer costMs;
    private String createBy;
    private LocalDateTime createTime;
}
