package com.zh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zh.domain.dto.ProductDTO;
import com.zh.domain.dto.Result;
import com.zh.domain.po.Inventory;
import com.zh.domain.po.OrderItem;
import com.zh.domain.po.Orders;
import com.zh.domain.po.Product;
import com.zh.mapper.ProductMapper;
import com.zh.service.IInventoryService;
import com.zh.service.IOrderItemService;
import com.zh.service.IOrdersService;
import com.zh.service.IProductService;


import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author zhlyu
 * @since 2026-07-31
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {

    @Autowired
    private IInventoryService inventoryService;

    @Autowired
    private IOrderItemService orderItemService;

    @Autowired
    private IOrdersService ordersService;

    @Override
    public Result searchProducts(String name, Integer page, Integer size) {
        // 1.前端page从0开始，MyBatis-Plus Page从1开始，需要+1
        Page<Product> p = new Page<>(page + 1, size);
        // 2.按名称模糊查询，name为空则查全部
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), Product::getName, name)
                .orderByAsc(Product::getId);
        // 3.分页查询（依赖你已配置的分页插件）
        return Result.ok(this.page(p, wrapper));
    }



    @Override
    public Result addProduct(ProductDTO dto) {
        // 1.基础校验
        if (StrUtil.isBlank(dto.getName()) || dto.getPrice() == null) {
            return Result.fail("商品名称和价格不能为空");
        }
        // 2.DTO转PO，并填充时间字段
        Product product = new Product();
        product.setSku(dto.getSku());
        product.setName(dto.getName());
        product.setCategory(dto.getCategory());
        product.setUnit(dto.getUnit());
        product.setPrice(dto.getPrice());
        product.setLowStockThreshold(dto.getLowStockThreshold());
        LocalDateTime now = LocalDateTime.now();
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        // 3.保存
        return this.save(product) ? Result.ok() : Result.fail("新增商品失败");
    }

    @Override
    public Result updateProduct(Long id, ProductDTO dto) {
        // 1.校验商品是否存在
        Product product = this.getById(id);
        if (product == null) {
            return Result.fail("商品不存在");
        }
        // 2.更新字段（与新增字段一致）
        product.setSku(dto.getSku());
        product.setName(dto.getName());
        product.setCategory(dto.getCategory());
        product.setUnit(dto.getUnit());
        product.setPrice(dto.getPrice());
        product.setLowStockThreshold(dto.getLowStockThreshold());
        product.setUpdatedAt(LocalDateTime.now());
        // 3.保存
        return this.updateById(product) ? Result.ok() : Result.fail("更新商品失败");
    }

    @Override
    public Result deleteProduct(Long id) {
        // 1.校验商品存在
        Product product = this.getById(id);
        if (product == null) {
            return Result.fail("商品不存在");
        }
//        TODO 添加限制
        // 2.限制一：库存大于0不允许删除
        Inventory inv = inventoryService.lambdaQuery()
                .eq(Inventory::getProductId, id)
                .one();
        if (inv != null && inv.getQuantity() > 0) {
            return Result.fail("商品库存大于0，不允许删除");
        }
        // 3.限制二：存在待处理入库/出库单不允许删除
        List<OrderItem> items = orderItemService.lambdaQuery()
                .eq(OrderItem::getProductId, id)
                .list();
        if (!items.isEmpty()) {
            List<Long> orderIds = items.stream()
                    .map(OrderItem::getOrderId)
                    .distinct()
                    .toList();
            long pendingCount = ordersService.lambdaQuery()
                    .in(Orders::getId, orderIds)
                    .ne(Orders::getStatus, "CANCELLED")
                    .count();
            if (pendingCount > 0) {
                return Result.fail("存在待处理的入库/出库单，不允许删除");
            }
        }
        // 4.通过全部校验，删除
        return this.removeById(id) ? Result.ok() : Result.fail("删除商品失败");
    }
}
