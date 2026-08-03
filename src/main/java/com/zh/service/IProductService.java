package com.zh.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zh.domain.dto.ProductDTO;
import com.zh.domain.dto.Result;
import com.zh.domain.po.Product;


/**
 * <p>
 * 商品表 服务类
 * </p>
 *
 * @author zhlyu
 * @since 2026-07-31
 */
public interface IProductService extends IService<Product> {

    Result searchProducts(String name, Integer page, Integer size);


    Result addProduct(ProductDTO dto);

    Result updateProduct(Long id, ProductDTO dto);

    Result deleteProduct(Long id);
}
