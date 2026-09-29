package com.zh.domain.dto;

import lombok.Data;

@Data
public class InventoryAdjustDTO {
    private Long productId;
    private Integer quantity;
    private String remark;
}
