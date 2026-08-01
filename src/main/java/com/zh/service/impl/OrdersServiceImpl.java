package com.zh.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zh.domain.po.Orders;
import com.zh.mapper.OrdersMapper;
import com.zh.service.IOrdersService;

import org.springframework.stereotype.Service;

/**
 * <p>
 * 订单表 服务实现类
 * </p>
 *
 * @author zhlyu
 * @since 2026-07-31
 */
@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements IOrdersService {

}
