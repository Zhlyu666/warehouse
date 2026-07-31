package com.zh.service.impl;

import com.zh.domain.po.InventoryLog;
import com.zh.mapper.InventoryLogMapper;
import com.zh.service.IInventoryLogService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 库存流水表 服务实现类
 * </p>
 *
 * @author zhlyu
 * @since 2026-07-31
 */
@Service
public class InventoryLogServiceImpl extends ServiceImpl<InventoryLogMapper, InventoryLog> implements IInventoryLogService {

}
