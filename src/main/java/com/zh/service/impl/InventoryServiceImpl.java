package com.zh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zh.domain.dto.InventoryAdjustDTO;
import com.zh.domain.po.InventoryLog;
import com.zh.domain.vo.InventoryVO;
import com.zh.domain.dto.Result;
import com.zh.domain.po.Inventory;
import com.zh.domain.po.Product;
import com.zh.domain.vo.InventoryVO;
import com.zh.mapper.InventoryMapper;
import com.zh.mapper.ProductMapper;
import com.zh.service.IInventoryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zh.utils.InventoryStatus.LOW_STOCK;
import static com.zh.utils.InventoryStatus.NORMAL;

/**
 * <p>
 * 库存表 服务实现类
 * </p>
 *
 * @author zhlyu
 * @since 2026-07-31
 */
@Service
public class InventoryServiceImpl extends ServiceImpl<InventoryMapper, Inventory> implements IInventoryService {

    @Autowired
    private ProductMapper productMapper;

    @Override
    public Result searchInventory(String keyword, boolean lowStockOnly) {
        // 1.按名称模糊查询商品
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .like(StringUtils.hasText(keyword), Product::getName, keyword));
        if (products.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 2.批量查询库存，按商品ID组装Map
        List<Long> productIds = products.stream().map(Product::getId).toList();
        List<Inventory> inventories = this.lambdaQuery()
                .in(Inventory::getProductId, productIds)
                .list();
        Map<Long, Inventory> invMap = inventories.stream()
                .collect(Collectors.toMap(Inventory::getProductId, i -> i, (a, b) -> a));
        // 3.组装VO
        List<InventoryVO> result = new ArrayList<>();
        for (Product p : products) {
            Inventory inv = invMap.get(p.getId());
            if (inv == null) {
                continue; // 无库存记录的商品不展示
            }
            // 可售库存 = 总库存 - 已预留
            int available = inv.getQuantity() - inv.getReservedQuantity();
            // 低库存判断：可售库存 <= 低库存阈值
            int threshold = p.getLowStockThreshold() == null ? 0 : p.getLowStockThreshold();
            String status = available <= threshold ? LOW_STOCK : NORMAL;
            // 低库存过滤
            if (lowStockOnly && NORMAL.equals(status)) {
                continue;
            }
            InventoryVO vo = new InventoryVO();
            vo.setProductId(p.getId());
            vo.setSku(p.getSku());
            vo.setName(p.getName());
            vo.setCategory(p.getCategory());
            vo.setQuantity(inv.getQuantity());
            vo.setReservedQuantity(inv.getReservedQuantity());
            vo.setAvailableQuantity(available);
            vo.setLowStockThreshold(threshold);
            vo.setStatus(status);
            result.add(vo);
        }
        return Result.ok(result);
    }

    @Override
    @Transactional
    public Result adjustInventory(InventoryAdjustDTO dto) {
        // 1.参数校验
        if (dto.getProductId() == null) {
            return Result.fail("商品ID不能为空");
        }
        if (dto.getQuantity() == null || dto.getQuantity() == 0) {
            return Result.fail("调整数量不能为空且不能为0");
        }
        Integer delta = dto.getQuantity();
        // 2.查询现有库存记录
        Inventory inv = this.lambdaQuery()
                .eq(Inventory::getProductId, dto.getProductId())
                .one();
        if (inv == null) {
            // 无库存记录：入库可自动创建，扣减直接报错
            if (delta < 0) {
                return Result.fail("该商品无库存记录，无法扣减");
            }
            Inventory newInv = new Inventory();
            newInv.setProductId(dto.getProductId());
            newInv.setQuantity(delta);
            newInv.setReservedQuantity(0);
            newInv.setUpdatedAt(LocalDateTime.now());
            this.save(newInv);
            saveLog(dto, 0, delta);
            return Result.ok();
        }
        // 3.原子更新库存：保证 quantity + delta >= 0（并发下也不会超扣）
        boolean rows = this.update(new LambdaUpdateWrapper<Inventory>()
                .eq(Inventory::getId, inv.getId())
                .ge(Inventory::getQuantity, -delta)
                .setSql("quantity = quantity + {0}", delta)
                .set(Inventory::getUpdatedAt, LocalDateTime.now()));
        if (!rows) {
            return Result.fail("库存不足，调整失败");
        }
        // 4.查询调整后库存，写入流水
        Inventory after = this.getById(inv.getId());
        saveLog(dto, after.getQuantity() - delta, after.getQuantity());
        return Result.ok();
    }

    private void saveLog(InventoryAdjustDTO dto, int before, int after) {
        InventoryLog log = new InventoryLog();
        log.setProductId(dto.getProductId());
        log.setChangeType("ADJUST");
        log.setChangeQuantity(dto.getQuantity());
        log.setBeforeQuantity(before);
        log.setAfterQuantity(after);
        log.setRemark(dto.getRemark());
        log.setCreatedAt(LocalDateTime.now());
//        inventoryLogService.save(log);
    }
}
