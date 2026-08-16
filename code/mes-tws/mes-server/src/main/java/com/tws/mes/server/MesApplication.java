package com.tws.mes.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * TWS 整机装配 MES 启动类。
 * Maven 多模块单体：本模块聚合 base/execution/quality/trace，打成单个可运行 jar。
 * 注意：默认只扫描本包，必须显式扩大到 com.tws.mes 才能装配各业务模块的 Bean。
 */
@SpringBootApplication(scanBasePackages = "com.tws.mes")
@MapperScan("com.tws.mes.**.mapper")
@EnableScheduling
public class MesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MesApplication.class, args);
    }
}
