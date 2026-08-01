package com.zh.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zh.domain.po.Product;
import com.zh.mapper.ProductMapper;
import com.zh.service.IProductService;

import org.springframework.stereotype.Service;

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

}
