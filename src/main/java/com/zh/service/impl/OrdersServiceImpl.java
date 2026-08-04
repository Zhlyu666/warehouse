package com.zh.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zh.common.exception.InsufficientStockException;
import com.zh.domain.dto.OrderCreateDTO;
import com.zh.domain.dto.OrderItemDTO;
import com.zh.domain.dto.Result;
import com.zh.domain.dto.UserDTO;
import com.zh.domain.po.Inventory;
import com.zh.domain.po.OrderItem;
import com.zh.domain.po.Orders;
import com.zh.domain.po.Product;
import com.zh.domain.vo.OrderItemVO;
import com.zh.domain.vo.OrderVO;
import com.zh.mapper.OrdersMapper;
import com.zh.mapper.ProductMapper;
import com.zh.service.IInventoryService;
import com.zh.service.IOrderItemService;
import com.zh.service.IOrdersService;

import com.zh.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zh.utils.OrderStatus.CANCELLED;
import static com.zh.utils.OrderStatus.CONFIRMING;
import static com.zh.utils.OrderStatus.CREATED;
import static com.zh.utils.OrderStatus.INBOUND;
import static com.zh.utils.OrderStatus.OUTBOUND;
import static com.zh.utils.OrderStatus.RESERVED;

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
    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private IOrderItemService orderItemService;

    @Autowired
    private IInventoryService inventoryService;

    /**
     * 创建入库单：校验明细与商品后建单（状态CREATED），不操作库存
     */
    @Override
    @Transactional
    public Result createInboundOrder(OrderCreateDTO dto) {
        // 1.校验
        validateItems(dto.getItems(), "入库");
        // 2.查商品（含存在性校验）
        Map<Long, Product> productMap = getProductMap(dto.getItems());
        // 3.建单（CREATED，不操作库存）
        Orders order = createOrder(generateOrderNo("IN"), INBOUND, CREATED, dto);
        // 4.存明细 + 组VO返回
        return Result.ok(buildOrderVO(order, saveOrderItems(order.getId(), dto.getItems(), productMap)));
    }

    /**
     * 创建出库单：校验明细与商品并预留库存成功后建单（状态RESERVED）
     */
    @Override
    @Transactional
    public Result createOutboundOrder(OrderCreateDTO dto) {
        // 1.校验
        validateItems(dto.getItems(), "出库");
        // 2.查商品（含存在性校验）
        Map<Long, Product> productMap = getProductMap(dto.getItems());
        // 3.预留库存（失败抛409）
        reserveStock(dto.getItems());
        // 4.建单（RESERVED）
        Orders order = createOrder(generateOrderNo("OUT"), OUTBOUND, RESERVED, dto);
        // 5.存明细 + 组VO返回
        return Result.ok(buildOrderVO(order, saveOrderItems(order.getId(), dto.getItems(), productMap)));
    }

    /**
     * 确认入库单：校验状态为CREATED后流转为CONFIRMING，库存异步更新
     */
    @Override
    @Transactional
    public Result confirmInboundOrder(Long id) {
        // 1.校验入库单存在且可确认
        Orders order = getInboundOrder(id);
        if (!CREATED.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅CREATED状态的入库单可确认");
        }
        // 2.状态流转：CREATED -> CONFIRMING，接口立即返回
        order.setStatus(CONFIRMING);
        this.updateById(order);
        // TODO: 更新库存与写库存流水由RabbitMQ消费者异步完成（mq功能暂未实现）
        return Result.ok();
    }

    /**
     * 确认出库单：校验状态为RESERVED后流转为CONFIRMING，库存异步扣减
     */
    @Override
    @Transactional
    public Result confirmOutboundOrder(Long id) {
        // 1.校验出库单存在且可确认
        Orders order = getOutboundOrder(id);
        if (!RESERVED.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅RESERVED状态的出库单可确认");
        }
        // 2.状态流转：RESERVED -> CONFIRMING，接口立即返回
        order.setStatus(CONFIRMING);
        this.updateById(order);
        // TODO: 扣减库存与写库存流水由RabbitMQ消费者异步完成（mq功能暂未实现）
        return Result.ok();
    }

    /**
     * 取消出库单：校验状态为RESERVED后释放预留库存，状态流转为CANCELLED
     */
    @Override
    @Transactional
    public Result cancelOutboundOrder(Long id) {
        // 1.校验出库单存在且可取消
        Orders order = getOutboundOrder(id);
        if (!RESERVED.equals(order.getStatus())) {
            throw new IllegalArgumentException("仅RESERVED状态的出库单可取消");
        }
        // 2.释放预留库存（原子条件更新，保证不为负）
        List<OrderItem> items = orderItemService.lambdaQuery()
                .eq(OrderItem::getOrderId, id)
                .list();
        for (OrderItem item : items) {
            boolean released = inventoryService.update(new LambdaUpdateWrapper<Inventory>()
                    .eq(Inventory::getProductId, item.getProductId())
                    .apply("reserved_quantity >= {0}", item.getQuantity())
                    .setSql("reserved_quantity = reserved_quantity - {0}", item.getQuantity())
                    .set(Inventory::getUpdatedAt, LocalDateTime.now()));
            if (!released) {
                throw new IllegalStateException("商品[" + item.getProductId() + "]预留库存数据异常");
            }
        }
        // 3.状态流转：RESERVED -> CANCELLED
        order.setStatus(CANCELLED);
        this.updateById(order);
        return Result.ok();
    }

    /**
     * 查询订单列表：按类型/状态可选条件过滤，返回订单及明细，按创建时间倒序
     */
    @Override
    public Result listOrders(String type, String status) {
        // 1.条件查询（type/status 均为可选）
        List<Orders> orders = this.lambdaQuery()
                .eq(StringUtils.hasText(type), Orders::getType, type)
                .eq(StringUtils.hasText(status), Orders::getStatus, status)
                .orderByDesc(Orders::getCreatedAt)
                .list();
        if (orders.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 2.批量查询明细，按订单ID分组
        List<Long> orderIds = orders.stream().map(Orders::getId).toList();
        Map<Long, List<OrderItem>> itemMap = orderItemService.lambdaQuery()
                .in(OrderItem::getOrderId, orderIds)
                .list()
                .stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        // 3.组装VO（复用buildOrderVO）
        List<OrderVO> vos = orders.stream()
                .map(o -> buildOrderVO(o, itemMap.getOrDefault(o.getId(), Collections.emptyList())))
                .toList();
        return Result.ok(vos);
    }

    private Orders getOutboundOrder(Long id) {
        Orders order = this.getById(id);
        if (order == null || !OUTBOUND.equals(order.getType())) {
            throw new IllegalArgumentException("出库单不存在");
        }
        return order;
    }

    // ==================== 以下为入库/出库共用的私有方法 ====================

    private Orders getInboundOrder(Long id) {
        Orders order = this.getById(id);
        if (order == null || !INBOUND.equals(order.getType())) {
            throw new IllegalArgumentException("入库单不存在");
        }
        return order;
    }

    private void validateItems(List<OrderItemDTO> items, String bizName) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException(bizName + "明细不能为空");
        }
        for (OrderItemDTO item : items) {
            if (item.getProductId() == null) {
                throw new IllegalArgumentException("商品ID不能为空");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("商品数量必须大于0");
            }
        }
    }

    private Map<Long, Product> getProductMap(List<OrderItemDTO> items) {
        List<Long> productIds = items.stream()
                .map(OrderItemDTO::getProductId).distinct().toList();
        Map<Long, Product> map = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
        for (Long pid : productIds) {
            if (!map.containsKey(pid)) {
                throw new IllegalArgumentException("商品ID不存在：" + pid);
            }
        }
        return map;
    }

    private void reserveStock(List<OrderItemDTO> items) {
        for (OrderItemDTO item : items) {
            boolean reserved = inventoryService.update(new LambdaUpdateWrapper<Inventory>()
                    .eq(Inventory::getProductId, item.getProductId())
                    .apply("quantity - reserved_quantity >= {0}", item.getQuantity())
                    .setSql("reserved_quantity = reserved_quantity + {0}", item.getQuantity())
                    .set(Inventory::getUpdatedAt, LocalDateTime.now()));
            if (!reserved) {
                throw new InsufficientStockException("商品[" + item.getProductId() + "]可用库存不足");
            }
        }
    }

    private String generateOrderNo(String prefix) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        long count = this.lambdaQuery().likeRight(Orders::getOrderNo, prefix + date).count();
        return String.format("%s%s%04d", prefix, date, count + 1);
    }

    private Orders createOrder(String orderNo, String type, String status, OrderCreateDTO dto) {
        Orders order = new Orders();
        order.setOrderNo(orderNo);
        order.setType(type);
        order.setStatus(status);
        order.setTotalQuantity(dto.getItems().stream().mapToInt(OrderItemDTO::getQuantity).sum());
        UserDTO user = UserHolder.getUser();
        order.setOperatorId(user == null ? null : user.getId());
        order.setOperatorName(user == null || user.getUsername() == null ? "管理员" : user.getUsername());
        order.setRemark(dto.getRemark());
        order.setCreatedAt(LocalDateTime.now());
        this.save(order);
        return order;
    }

    private List<OrderItem> saveOrderItems(Long orderId, List<OrderItemDTO> items, Map<Long, Product> productMap) {
        List<OrderItem> itemList = new ArrayList<>();
        for (OrderItemDTO item : items) {
            OrderItem oi = new OrderItem();
            oi.setOrderId(orderId);
            oi.setProductId(item.getProductId());
            oi.setProductName(productMap.get(item.getProductId()).getName());
            oi.setQuantity(item.getQuantity());
            itemList.add(oi);
        }
        orderItemService.saveBatch(itemList);
        return itemList;
    }

    private OrderVO buildOrderVO(Orders order, List<OrderItem> itemList) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setType(order.getType());
        vo.setStatus(order.getStatus());
        vo.setTotalQuantity(order.getTotalQuantity());
        vo.setOperatorName(order.getOperatorName());
        vo.setRemark(order.getRemark());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setConfirmedAt(order.getConfirmedAt());
        vo.setItems(itemList.stream().map(oi -> {
            OrderItemVO iv = new OrderItemVO();
            iv.setProductId(oi.getProductId());
            iv.setProductName(oi.getProductName());
            iv.setQuantity(oi.getQuantity());
            return iv;
        }).toList());
        return vo;
    }
}
