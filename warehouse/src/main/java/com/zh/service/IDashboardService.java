package com.zh.service;

import com.zh.domain.dto.Result;

/**
 * 看板汇总服务
 */
public interface IDashboardService {

    /**
     * 看板汇总：商品数、库存总量、低库存数、待处理单数
     */
    Result getSummary();
}
