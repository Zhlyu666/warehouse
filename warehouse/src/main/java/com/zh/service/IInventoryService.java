package com.zh.service;


import com.baomidou.mybatisplus.spring.service.IService;
import com.zh.domain.dto.InventoryAdjustDTO;
import com.zh.domain.dto.Result;
import com.zh.domain.po.Inventory;


/**
 * <p>
 * 库存表 服务类
 * </p>
 *
 * @author zhlyu
 * @since 2026-07-31
 */
public interface IInventoryService extends IService<Inventory> {

    Result searchInventory(String keyword, boolean lowStockOnly);
    Result adjustInventory(InventoryAdjustDTO dto);
}
