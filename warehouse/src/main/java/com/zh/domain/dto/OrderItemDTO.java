package com.zh.domain.dto;

import lombok.Data;

@Data
public class OrderItemDTO {
    private Long productId;
    private Integer quantity;
}
