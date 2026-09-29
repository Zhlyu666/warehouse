package com.zh.domain.vo;

import lombok.Data;

@Data
public class InventoryVO {
    private Long productId;
    private String sku;
    private String name;
    private String category;
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private Integer lowStockThreshold;
    private String status;
}
