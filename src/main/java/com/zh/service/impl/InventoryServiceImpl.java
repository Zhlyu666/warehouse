package com.zh.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zh.domain.dto.InventoryAdjustDTO;
import com.zh.domain.dto.Result;
import com.zh.domain.po.Inventory;
import com.zh.domain.po.InventoryLog;
import com.zh.domain.po.Product;
import com.zh.domain.vo.InventoryVO;
import com.zh.mapper.InventoryMapper;
import com.zh.mapper.ProductMapper;
import com.zh.service.IInventoryService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static cn.hutool.core.thread.ThreadUtil.sleep;
import static com.zh.utils.InventoryStatus.LOW_STOCK;
import static com.zh.utils.InventoryStatus.NORMAL;
import static com.zh.utils.RedisConstants.INVENTORY_LOCK_KEY_PREFIX;
import static com.zh.utils.RedisConstants.INVENTORY_VIEW_KEY_PREFIX;

/**
 * <p>
 * 库存表 服务实现类
 * </p>
 *
 * @author zhlyu
 * @since 2026-07-31
 */
@Service
@Slf4j
public class InventoryServiceImpl extends ServiceImpl<InventoryMapper, Inventory> implements IInventoryService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public Result searchInventory(String keyword, boolean lowStockOnly) {
        // 1.按名称模糊查询商品
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .like(StringUtils.hasText(keyword), Product::getName, keyword));
        if (products.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> productIds = products.stream().map(Product::getId).toList();
        // 2.批量读缓存：命中的直接用，未命中的收集待回源
        Map<Long, InventoryVO> voMap = new HashMap<>();
        List<Long> missIds = new ArrayList<>();
        for (Long pid : productIds) {
            String json = stringRedisTemplate.opsForValue().get(INVENTORY_VIEW_KEY_PREFIX + pid);
            if (StrUtil.isNotBlank(json)) {
                try {
                    voMap.put(pid, objectMapper.readValue(json, InventoryVO.class));
                } catch (JsonProcessingException e) {
                    missIds.add(pid); // 缓存数据异常，回源数据库
                }
            } else {
                missIds.add(pid);
            }
        }
        // 3.未命中的批量查库存并写入缓存（TTL 60秒）
        if (!missIds.isEmpty()) {
            Map<Long, Inventory> invMap = this.lambdaQuery()
                    .in(Inventory::getProductId, missIds)
                    .list()
                    .stream()
                    .collect(Collectors.toMap(Inventory::getProductId, i -> i, (a, b) -> a));
            for (Product p : products) {
                if (!missIds.contains(p.getId())) {
                    continue;
                }
                Inventory inv = invMap.get(p.getId());
                if (inv == null) {
                    continue; // 无库存记录的商品不展示
                }
                InventoryVO vo = buildVO(p, inv);
                voMap.put(p.getId(), vo);
                try {
                    stringRedisTemplate.opsForValue().set(
                            INVENTORY_VIEW_KEY_PREFIX + p.getId(),
                            objectMapper.writeValueAsString(vo),
                            Duration.ofSeconds(60));
                } catch (JsonProcessingException e) {
                    log.warn("库存视图缓存序列化失败, productId={}", p.getId(), e);
                }
            }
        }
        // 4.组装结果（低库存过滤）
        List<InventoryVO> result = new ArrayList<>();
        for (Product p : products) {
            InventoryVO vo = voMap.get(p.getId());
            if (vo == null) {
                continue;
            }
            if (lowStockOnly && NORMAL.equals(vo.getStatus())) {
                continue;
            }
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
        // 2.获取分布式锁：防止同一商品并发调整
        RLock lock = redissonClient.getLock(INVENTORY_LOCK_KEY_PREFIX + dto.getProductId());
        boolean locked;
        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.fail("获取库存操作锁被中断，请稍后重试");
        }
        if (!locked) {
            return Result.fail("该商品正在被其他操作处理，请稍后重试");
        }
        try {
            return doAdjust(dto);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private Result doAdjust(InventoryAdjustDTO dto) {
        Integer delta = dto.getQuantity();
        // 1.查询现有库存记录
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
            // 2.删除缓存，防止读到旧数据
            stringRedisTemplate.delete(INVENTORY_VIEW_KEY_PREFIX + dto.getProductId());
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
        // 5.删除缓存，防止读到旧数据
        stringRedisTemplate.delete(INVENTORY_VIEW_KEY_PREFIX + dto.getProductId());
        return Result.ok();
    }

    private InventoryVO buildVO(Product p, Inventory inv) {
        // 可售库存 = 总库存 - 已预留
        int available = inv.getQuantity() - inv.getReservedQuantity();
        // 低库存判断：可售库存 <= 低库存阈值
        int threshold = p.getLowStockThreshold() == null ? 0 : p.getLowStockThreshold();
        String status = available <= threshold ? LOW_STOCK : NORMAL;
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
        return vo;
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
