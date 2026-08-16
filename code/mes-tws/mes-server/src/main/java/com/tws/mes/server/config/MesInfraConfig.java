package com.tws.mes.server.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * MyBatis-Plus 分页插件 + 异步线程池。
 *
 * 异步线程池的用途：过站主流程"同步落库 + 校验"，良率统计/工单进度等
 * 非关键路径走事件异步更新（@Async），压低过站接口 P95 —— 与"生产版用 MQ
 * 削峰"的叙事对应，学习版以 Spring 事件替位，接口不变，将来可平滑换 MQ。
 */
@Configuration
@EnableAsync
public class MesInfraConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 乐观锁：实体字段加 @Version 后，UPDATE 自动带 version 条件并自增
        interceptor.addInnerInterceptor(new com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    @Bean("mesExecutor")
    public Executor mesExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("mes-async-");
        executor.initialize();
        return executor;
    }
}
