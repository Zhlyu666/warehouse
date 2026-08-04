package com.zh.domain.vo;

import lombok.Data;

/**
 * 看板汇总数据
 */
@Data
public class DashboardSummaryVO {

    /**
     * 商品总数
     */
    private Long productCount;

    /**
     * 库存总量（所有商品 quantity 总和）
     */
    private Long totalInventory;

    /**
     * 低库存商品数（可用库存 <= 阈值）
     */
    private Long lowStockCount;

    /**
     * 待处理入库单数（CREATED）
     */
    private Long pendingInbound;

    /**
     * 待处理出库单数（RESERVED）
     */
    private Long pendingOutbound;
}
