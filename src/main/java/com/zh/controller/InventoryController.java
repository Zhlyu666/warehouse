package com.zh.controller;


import com.zh.domain.dto.InventoryAdjustDTO;
import com.zh.domain.dto.Result;
import com.zh.service.IInventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.relational.core.sql.In;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 库存表 前端控制器
 * </p>
 *
 * @author zhlyu
 * @since 2026-07-31
 */
@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private IInventoryService inventoryService;
    @GetMapping
    public Result search(@RequestParam(value = "keyword", required = false) String keyword,
                         @RequestParam(value = "lowStockOnly", defaultValue = "false") boolean lowStockOnly) {
        return inventoryService.searchInventory(keyword, lowStockOnly);
    }


    @PostMapping("/adjust")
    public Result adjust(@RequestBody InventoryAdjustDTO dto) {
        return inventoryService.adjustInventory(dto);
    }
}
