package com.zh.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zh.common.config.RabbitConfig;
import com.zh.domain.dto.OrderConfirmMessage;
import com.zh.service.IOrderConfirmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 订单确认消息消费者：接收JSON字符串，反序列化后调用Service处理（事务在Service层）
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderConfirmConsumer {

    private final IOrderConfirmService orderConfirmService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitConfig.INBOUND_CONFIRM_QUEUE)
    public void onInboundConfirm(String json) {
        try {
            OrderConfirmMessage msg = objectMapper.readValue(json, OrderConfirmMessage.class);
            orderConfirmService.processInboundConfirm(msg);
        } catch (JsonProcessingException e) {
            log.error("入库确认消息反序列化失败: {}", json, e);
        }
    }

    @RabbitListener(queues = RabbitConfig.OUTBOUND_CONFIRM_QUEUE)
    public void onOutboundConfirm(String json) {
        try {
            OrderConfirmMessage msg = objectMapper.readValue(json, OrderConfirmMessage.class);
            orderConfirmService.processOutboundConfirm(msg);
        } catch (JsonProcessingException e) {
            log.error("出库确认消息反序列化失败: {}", json, e);
        }
    }
}
