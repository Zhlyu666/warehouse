package com.zh.mq;

import com.zh.common.config.RabbitConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 低库存告警消费者：本期记录告警日志，预留短信/邮件通知通道
 */
@Component
@Slf4j
public class AlertConsumer {

    @RabbitListener(queues = RabbitConfig.ALERT_QUEUE)
    public void onAlert(String json) {
        log.warn("收到低库存告警: {}", json);
    }
}
