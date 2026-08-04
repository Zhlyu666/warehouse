package com.zh.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderCreateDTO {
    private String remark;
    private List<OrderItemDTO> items;
}
