package com.zh.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单确认消息体：确认入库/出库后发送，消费者凭orderId回查订单与明细
 */
@Data
public class OrderConfirmMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID（核心字段，消费者据此回查数据库）
     */
    private Long orderId;

    /**
     * 订单类型：INBOUND / OUTBOUND
     */
    private String type;

    /**
     * 确认时间
     */
    private LocalDateTime confirmTime;
}
