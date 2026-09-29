package com.zh.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zh.common.config.RabbitConfig;
import com.zh.domain.dto.OrderConfirmMessage;
import com.zh.domain.dto.StockAlertMessage;
import com.zh.domain.po.Inventory;
import com.zh.domain.po.InventoryLog;
import com.zh.domain.po.OrderItem;
import com.zh.domain.po.Orders;
import com.zh.domain.po.Product;
import com.zh.service.IInventoryLogService;
import com.zh.service.IInventoryService;
import com.zh.service.IOrderConfirmService;
import com.zh.service.IOrderItemService;
import com.zh.service.IOrdersService;
import com.zh.service.IProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.zh.utils.OrderStatus.CONFIRMED;
import static com.zh.utils.OrderStatus.CONFIRMING;
import static com.zh.utils.RedisConstants.ALERT_KEY_PREFIX;
import static com.zh.utils.RedisConstants.INVENTORY_VIEW_KEY_PREFIX;

/**
 * 订单确认消息处理实现：双防线幂等（状态+流水）保证重复消费安全
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderConfirmServiceImpl implements IOrderConfirmService {

    private final IOrdersService ordersService;
    private final IOrderItemService orderItemService;
    private final IInventoryService inventoryService;
    private final IInventoryLogService inventoryLogService;
    private final IProductService productService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 入库确认：库存增加（无记录则创建）、写流水、状态COMPLETED、删缓存、低库存告警
     */
    @Override
    @Transactional
    public void processInboundConfirm(OrderConfirmMessage msg) {
        Orders order = checkAndGetOrder(msg);
        if (order == null) {
            return;
        }
        List<OrderItem> items = orderItemService.lambdaQuery()
                .eq(OrderItem::getOrderId, order.getId()).list();
        for (OrderItem item : items) {
            Inventory inv = inventoryService.lambdaQuery()
                    .eq(Inventory::getProductId, item.getProductId()).one();
            int before = inv == null ? 0 : inv.getQuantity();
            if (inv == null) {
                // 无库存记录：创建
                Inventory newInv = new Inventory();
                newInv.setProductId(item.getProductId());
                newInv.setQuantity(item.getQuantity());
                newInv.setReservedQuantity(0);
                newInv.setUpdatedAt(LocalDateTime.now());
                inventoryService.save(newInv);
            } else {
                // 有记录：原子增加
                inventoryService.update(new LambdaUpdateWrapper<Inventory>()
                        .eq(Inventory::getId, inv.getId())
                        .setSql("quantity = quantity + {0}", item.getQuantity())
                        .set(Inventory::getUpdatedAt, LocalDateTime.now()));
            }
            Inventory after = inventoryService.lambdaQuery()
                    .eq(Inventory::getProductId, item.getProductId()).one();
            saveLog(item, "INBOUND", item.getQuantity(), before, after.getQuantity(), order.getId());
            stringRedisTemplate.delete(INVENTORY_VIEW_KEY_PREFIX + item.getProductId());
            checkAndSendAlert(item.getProductId());
        }
        completeOrder(order);
        log.info("入库确认处理完成, orderId={}", order.getId());
    }

    /**
     * 出库确认：总库存与预留库存同时扣减、写流水、状态COMPLETED、删缓存、低库存告警
     */
    @Override
    @Transactional
    public void processOutboundConfirm(OrderConfirmMessage msg) {
        Orders order = checkAndGetOrder(msg);
        if (order == null) {
            return;
        }
        List<OrderItem> items = orderItemService.lambdaQuery()
                .eq(OrderItem::getOrderId, order.getId()).list();
        for (OrderItem item : items) {
            Inventory inv = inventoryService.lambdaQuery()
                    .eq(Inventory::getProductId, item.getProductId()).one();
            if (inv == null) {
                throw new IllegalStateException("商品[" + item.getProductId() + "]无库存记录，出库确认失败");
            }
            int before = inv.getQuantity();
            // 原子扣减：总库存与预留同时减（创建出库单时已预留，这里把"预留"变"实扣"）
            boolean ok = inventoryService.update(new LambdaUpdateWrapper<Inventory>()
                    .eq(Inventory::getProductId, item.getProductId())
                    .ge(Inventory::getQuantity, item.getQuantity())
                    .ge(Inventory::getReservedQuantity, item.getQuantity())
                    .setSql("quantity = quantity - {0}", item.getQuantity())
                    .setSql("reserved_quantity = reserved_quantity - {0}", item.getQuantity())
                    .set(Inventory::getUpdatedAt, LocalDateTime.now()));
            if (!ok) {
                throw new IllegalStateException("商品[" + item.getProductId() + "]出库扣减失败，库存数据异常");
            }
            Inventory after = inventoryService.lambdaQuery()
                    .eq(Inventory::getProductId, item.getProductId()).one();
            saveLog(item, "OUTBOUND", -item.getQuantity(), before, after.getQuantity(), order.getId());
            stringRedisTemplate.delete(INVENTORY_VIEW_KEY_PREFIX + item.getProductId());
            checkAndSendAlert(item.getProductId());
        }
        completeOrder(order);
        log.info("出库确认处理完成, orderId={}", order.getId());
    }

    /**
     * 幂等防线1+2：状态必须为CONFIRMING且流水不存在，否则跳过
     */
    private Orders checkAndGetOrder(OrderConfirmMessage msg) {
        // 防线1：单据状态判断
        Orders order = ordersService.getById(msg.getOrderId());
        if (order == null || !CONFIRMING.equals(order.getStatus())) {
            log.warn("确认消息跳过：订单不存在或状态非CONFIRMING, orderId={}, status={}",
                    msg.getOrderId(), order == null ? null : order.getStatus());
            return null;
        }
        // 防线2：库存流水判断（流水与库存更新在同一事务，流水存在即已处理）
        boolean exists = inventoryLogService.lambdaQuery()
                .eq(InventoryLog::getOrderId, order.getId()).exists();
        if (exists) {
            log.warn("确认消息跳过：库存流水已存在（重复消费）, orderId={}", order.getId());
            return null;
        }
        return order;
    }

    /**
     * 写库存流水（changeType: INBOUND正数 / OUTBOUND负数）
     */
    private void saveLog(OrderItem item, String changeType, int changeQuantity, int before, int after, Long orderId) {
        InventoryLog logRecord = new InventoryLog();
        logRecord.setProductId(item.getProductId());
        logRecord.setChangeType(changeType);
        logRecord.setChangeQuantity(changeQuantity);
        logRecord.setBeforeQuantity(before);
        logRecord.setAfterQuantity(after);
        logRecord.setOrderId(orderId);
        logRecord.setCreatedAt(LocalDateTime.now());
        inventoryLogService.save(logRecord);
    }

    /**
     * 订单状态流转：CONFIRMING -> COMPLETED
     */
    private void completeOrder(Orders order) {
        order.setStatus(CONFIRMED);
        order.setConfirmedAt(LocalDateTime.now());
        ordersService.updateById(order);
    }

    /**
     * 低库存告警：可用库存 <= 阈值时发送，Redis 防抖 10 分钟
     */
    private void checkAndSendAlert(Long productId) {
        Product product = productService.getById(productId);
        if (product == null) {
            return;
        }
        int threshold = product.getLowStockThreshold() == null ? 0 : product.getLowStockThreshold();
        Inventory inv = inventoryService.lambdaQuery()
                .eq(Inventory::getProductId, productId).one();
        if (inv == null) {
            return;
        }
        int available = inv.getQuantity() - inv.getReservedQuantity();
        if (available > threshold) {
            return;
        }
        // 防抖：10 分钟内同商品只告警一次
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(ALERT_KEY_PREFIX + productId, "1", Duration.ofMinutes(10));
        if (!Boolean.TRUE.equals(first)) {
            return;
        }
        StockAlertMessage alert = new StockAlertMessage();
        alert.setProductId(productId);
        alert.setAvailableQuantity(available);
        alert.setLowStockThreshold(threshold);
        alert.setAlertTime(LocalDateTime.now());
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.ORDER_EXCHANGE, RabbitConfig.ALERT_KEY,
                    objectMapper.writeValueAsString(alert));
            log.info("低库存告警已发送, productId={}, available={}", productId, available);
        } catch (JsonProcessingException e) {
            log.error("告警消息序列化失败, productId={}", productId, e);
        }
    }
}
