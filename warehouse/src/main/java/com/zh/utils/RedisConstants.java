package com.zh.utils;

public class RedisConstants {
    // 会话Key前缀
    public static final String SESSION_KEY_PREFIX = "warehouse:session:";
    // 库存视图缓存Key前缀
    public static final String INVENTORY_VIEW_KEY_PREFIX = "warehouse:inventory:view:";
    // 库存操作锁Key前缀
    public static final String INVENTORY_LOCK_KEY_PREFIX = "warehouse:lock:product:";
    // 低库存告警防抖Key前缀
    public static final String ALERT_KEY_PREFIX = "warehouse:alert:";
}
