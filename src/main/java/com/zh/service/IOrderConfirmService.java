package com.zh.service;

import com.zh.domain.dto.OrderConfirmMessage;

/**
 * 订单确认消息处理服务：由MQ消费者调用，事务边界在此层
 */
public interface IOrderConfirmService {

    /**
     * 处理入库确认：库存增加、写流水、状态COMPLETED
     */
    void processInboundConfirm(OrderConfirmMessage msg);

    /**
     * 处理出库确认：库存与预留同时扣减、写流水、状态COMPLETED
     */
    void processOutboundConfirm(OrderConfirmMessage msg);
}
