package com.zh.mapper;

import com.zh.domain.po.Inventory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 库存表 Mapper 接口
 * </p>
 *
 * @author zhlyu
 * @since 2026-07-31
 */
public interface InventoryMapper extends BaseMapper<Inventory> {

    /**
     * 低库存商品数：可用库存(quantity - reserved_quantity) <= 商品阈值
     * 口径与库存查询接口的LOW_STOCK计算一致
     */
    @Select("SELECT COUNT(*) FROM inventory i " +
            "JOIN product p ON i.product_id = p.id " +
            "WHERE i.quantity - i.reserved_quantity <= IFNULL(p.low_stock_threshold, 0)")
    Long countLowStock();
}
