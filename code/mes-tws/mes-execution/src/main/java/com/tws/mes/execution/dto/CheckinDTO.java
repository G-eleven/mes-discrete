package com.tws.mes.execution.dto;

import lombok.Data;

import java.util.Map;

/** 过站请求 DTO（扫码枪/模拟器提交） */
@Data
public class CheckinDTO {

    /** 扫的 SN（整机/部件） */
    private String sn;
    /** 工位码 */
    private String stationCode;
    /** OK / NG */
    private String result;
    /** NG 时必填：不良代码 */
    private String ngCode;
    /** 测试数据：firmware=1.2.5、mic_sensitivity=-37.2 等 */
    private Map<String, Object> testData;
}
