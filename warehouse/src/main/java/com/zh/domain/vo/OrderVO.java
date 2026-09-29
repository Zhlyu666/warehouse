package com.zh.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private String type;
    private String status;
    private Integer totalQuantity;
    private String operatorName;
    private String remark;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime confirmedAt;
    private List<OrderItemVO> items;
}
