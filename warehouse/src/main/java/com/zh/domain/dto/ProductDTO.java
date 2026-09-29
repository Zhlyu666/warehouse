package com.zh.domain.dto;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDTO {
    /**
     * 商品编码
     */
    private String sku;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品分类
     */
    private String category;


    /**
     * 计量单位
     */
    private String unit;

    /**
     * 单价
     */
    private BigDecimal price;

    /**
     * 低库存阈值
     */
    private Integer lowStockThreshold;
}
