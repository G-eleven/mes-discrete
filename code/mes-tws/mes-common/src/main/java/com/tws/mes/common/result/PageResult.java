package com.tws.mes.common.result;

import lombok.Data;

import java.util.List;

/** 分页返回结构 */
@Data
public class PageResult<T> {

    private long total;
    private List<T> list;

    public static <T> PageResult<T> of(long total, List<T> list) {
        PageResult<T> p = new PageResult<>();
        p.total = total;
        p.list = list;
        return p;
    }
}
