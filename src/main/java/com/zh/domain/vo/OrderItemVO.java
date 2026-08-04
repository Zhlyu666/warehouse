package com.zh.domain.vo;

import lombok.Data;

@Data
public class OrderItemVO {
    private Long productId;
    private String productName;
    private Integer quantity;
}
