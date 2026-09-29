package com.zh.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zh.domain.po.OrderItem;
import com.zh.mapper.OrderItemMapper;
import com.zh.service.IOrderItemService;

import org.springframework.stereotype.Service;

/**
 * <p>
 * 订单明细表 服务实现类
 * </p>
 *
 * @author zhlyu
 * @since 2026-07-31
 */
@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements IOrderItemService {

}
