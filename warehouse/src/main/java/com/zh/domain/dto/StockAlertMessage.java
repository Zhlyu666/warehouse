package com.zh.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 低库存告警消息体：库存变更后可用库存低于阈值时发送
 */
@Data
public class StockAlertMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 当前可售库存（quantity - reserved_quantity）
     */
    private Integer availableQuantity;

    /**
     * 低库存阈值
     */
    private Integer lowStockThreshold;

    /**
     * 告警时间
     */
    private LocalDateTime alertTime;
}
