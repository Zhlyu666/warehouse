package com.zh.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zh.domain.dto.OrderCreateDTO;
import com.zh.domain.dto.Result;
import com.zh.domain.po.Orders;


/**
 * <p>
 * 订单表 服务类
 * </p>
 *
 * @author zhlyu
 * @since 2026-07-31
 */
public interface IOrdersService extends IService<Orders> {
    Result createInboundOrder(OrderCreateDTO dto);

    Result createOutboundOrder(OrderCreateDTO dto);

    Result confirmInboundOrder(Long id);

    Result confirmOutboundOrder(Long id);

    Result cancelOutboundOrder(Long id);

    Result listOrders(String type, String status);
}
