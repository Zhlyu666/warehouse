package com.zh.controller;


import com.zh.domain.dto.OrderCreateDTO;
import com.zh.domain.dto.Result;
import com.zh.service.IOrdersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 订单表 前端控制器
 * </p>
 *
 * @author zhlyu
 * @since 2026-07-31
 */
@RestController
@RequestMapping("/orders")
public class OrdersController {
    @Autowired
    private IOrdersService ordersService;

    @PostMapping("/inbound")
    public Result createInbound(@RequestBody OrderCreateDTO dto) {
        return ordersService.createInboundOrder(dto);
    }

    @PostMapping("/inbound/{id}/confirm")
    public Result confirmInbound(@PathVariable Long id) {
        return ordersService.confirmInboundOrder(id);
    }

    @PostMapping("/outbound")
    public Result createOutbound(@RequestBody OrderCreateDTO dto) {
        return ordersService.createOutboundOrder(dto);
    }

    @PostMapping("/outbound/{id}/confirm")
    public Result confirmOutbound(@PathVariable Long id) {
        return ordersService.confirmOutboundOrder(id);
    }

    @PostMapping("/outbound/{id}/cancel")
    public Result cancelOutbound(@PathVariable Long id) {
        return ordersService.cancelOutboundOrder(id);
    }

    @GetMapping
    public Result list(@RequestParam(value = "type", required = false) String type,
                       @RequestParam(value = "status", required = false) String status) {
        return ordersService.listOrders(type, status);
    }
}
