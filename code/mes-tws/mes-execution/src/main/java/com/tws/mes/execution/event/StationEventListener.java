package com.tws.mes.execution.event;

import com.tws.mes.execution.entity.PlanWorkOrder;
import com.tws.mes.execution.mapper.PlanWorkOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 过站事件异步消费：更新工单 OK/NG 累计（只统计整机最后一道工序）。
 * 过站接口的同步路径只做"校验 + 流水落库"，统计类更新异步化，压低 P95。
 * 生产版把本监听器换成 MQ 消费者即可，主流程零改动。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StationEventListener {

    private final PlanWorkOrderMapper woMapper;

    @Async("mesExecutor")
    @EventListener
    public void onCheckin(StationCheckinEvent event) {
        try {
            if (!"MACHINE".equals(event.getSnType()) || !event.isLastOp()) {
                return;
            }
            PlanWorkOrder wo = woMapper.selectById(event.getWorkOrderId());
            if (wo == null) {
                return;
            }
            if ("OK".equals(event.getResult())) {
                wo.setOkQty((wo.getOkQty() == null ? 0 : wo.getOkQty()) + 1);
            } else {
                wo.setNgQty((wo.getNgQty() == null ? 0 : wo.getNgQty()) + 1);
            }
            woMapper.updateById(wo);
        } catch (Exception e) {
            // 异步消费失败不影响过站主流程；生产版进死信队列/重试，此处记日志
            log.error("过站事件处理失败: {}", event, e);
        }
    }
}
