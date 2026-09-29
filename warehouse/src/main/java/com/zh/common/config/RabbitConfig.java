package com.zh.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // 交换机
    public static final String ORDER_EXCHANGE = "warehouse.order.exchange";
    // 队列
    public static final String INBOUND_CONFIRM_QUEUE = "warehouse.inbound.confirm";
    public static final String OUTBOUND_CONFIRM_QUEUE = "warehouse.outbound.confirm";
    public static final String ALERT_QUEUE = "warehouse.alert";
    // 路由键
    public static final String INBOUND_KEY = "inbound.confirm";
    public static final String OUTBOUND_KEY = "outbound.confirm";
    public static final String ALERT_KEY = "alert";

    /**
     * 订单交换机：Direct类型，持久化
     */
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    /**
     * 入库确认队列：持久化 + Lazy（消息直接落盘）
     */
    @Bean
    public Queue inboundConfirmQueue() {
        return QueueBuilder.durable(INBOUND_CONFIRM_QUEUE).lazy().build();
    }

    /**
     * 出库确认队列：持久化 + Lazy
     */
    @Bean
    public Queue outboundConfirmQueue() {
        return QueueBuilder.durable(OUTBOUND_CONFIRM_QUEUE).lazy().build();
    }

    /**
     * 低库存告警队列：持久化 + Lazy
     */
    @Bean
    public Queue alertQueue() {
        return QueueBuilder.durable(ALERT_QUEUE).lazy().build();
    }

    /**
     * 绑定：入库确认队列 <- 路由键 inbound.confirm
     */
    @Bean
    public Binding inboundBinding() {
        return BindingBuilder.bind(inboundConfirmQueue()).to(orderExchange()).with(INBOUND_KEY);
    }

    /**
     * 绑定：出库确认队列 <- 路由键 outbound.confirm
     */
    @Bean
    public Binding outboundBinding() {
        return BindingBuilder.bind(outboundConfirmQueue()).to(orderExchange()).with(OUTBOUND_KEY);
    }

    /**
     * 绑定：告警队列 <- 路由键 alert
     */
    @Bean
    public Binding alertBinding() {
        return BindingBuilder.bind(alertQueue()).to(orderExchange()).with(ALERT_KEY);
    }
}
