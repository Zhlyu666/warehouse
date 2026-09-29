package com.zh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zh.domain.dto.Result;
import com.zh.domain.po.Inventory;
import com.zh.domain.po.Orders;
import com.zh.domain.vo.DashboardSummaryVO;
import com.zh.mapper.InventoryMapper;
import com.zh.mapper.OrdersMapper;
import com.zh.mapper.ProductMapper;
import com.zh.service.IDashboardService;
import com.zh.utils.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DashboardServiceImpl implements IDashboardService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private OrdersMapper ordersMapper;

    @Override
    public Result getSummary() {
        DashboardSummaryVO vo = new DashboardSummaryVO();
        // 1.商品总数
        vo.setProductCount(productMapper.selectCount(null));
        // 2.库存总量
        vo.setTotalInventory(queryTotalInventory());
        // 3.低库存商品数（可用库存 <= 阈值）
        vo.setLowStockCount(inventoryMapper.countLowStock());
        // 4.待处理入库单（CREATED）
        vo.setPendingInbound(ordersMapper.selectCount(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getType, OrderStatus.INBOUND)
                .eq(Orders::getStatus, OrderStatus.CREATED)));
        // 5.待处理出库单（RESERVED）
        vo.setPendingOutbound(ordersMapper.selectCount(new LambdaQueryWrapper<Orders>()
                .eq(Orders::getType, OrderStatus.OUTBOUND)
                .eq(Orders::getStatus, OrderStatus.RESERVED)));
        return Result.ok(vo);
    }

    /**
     * 查询库存总量（无库存时返回0）
     */
    private Long queryTotalInventory() {
        List<Map<String, Object>> list = inventoryMapper.selectMaps(
                new QueryWrapper<Inventory>().select("IFNULL(SUM(quantity), 0) AS total"));
        Object total = list.isEmpty() ? null : list.get(0).get("total");
        return total == null ? 0L : Long.parseLong(total.toString());
    }
}
