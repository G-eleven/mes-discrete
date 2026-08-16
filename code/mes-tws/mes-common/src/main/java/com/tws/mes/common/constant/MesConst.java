package com.tws.mes.common.constant;

/** 状态与枚举常量（与数据库注释保持一致，集中放一处便于查阅） */
public final class MesConst {

    private MesConst() {}

    /* ---- 工单状态机 ---- */
    public static final String WO_CREATED = "CREATED";         // 已创建
    public static final String WO_RELEASED = "RELEASED";       // 已下达
    public static final String WO_IN_PROGRESS = "IN_PROGRESS"; // 生产中
    public static final String WO_PAUSED = "PAUSED";           // 暂停
    public static final String WO_COMPLETED = "COMPLETED";     // 已完工
    public static final String WO_CLOSED = "CLOSED";           // 已关闭

    /* ---- SN 状态 ---- */
    public static final String SN_INIT = "INIT";       // 已注册未上线
    public static final String SN_IN_LINE = "IN_LINE"; // 在制
    public static final String SN_NG = "NG";           // 不良待修
    public static final String SN_RETEST = "RETEST";   // 维修后待重测
    public static final String SN_DONE = "DONE";       // 完工入库
    public static final String SN_SCRAP = "SCRAP";     // 报废

    /* ---- 过站流水类型 ---- */
    public static final String RT_CHECKIN = "CHECKIN";
    public static final String RT_LOADING = "LOADING";
    public static final String RT_BINDING = "BINDING";

    /* ---- 不良单状态 ---- */
    public static final String DEFECT_OPEN = "OPEN";
    public static final String DEFECT_REPAIRED = "REPAIRED";
    public static final String DEFECT_SCRAP = "SCRAP";
}
