package com.zh.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 库存流水表
 * </p>
 *
 * @author zhlyu
 * @since 2026-07-31
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("inventory_log")
public class InventoryLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流水ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 变动类型：INBOUND / OUTBOUND / ADJUST
     */
    private String changeType;

    /**
     * 变动数量（正=入库，负=出库/调整）
     */
    private Integer changeQuantity;

    /**
     * 变动前库存
     */
    private Integer beforeQuantity;

    /**
     * 变动后库存
     */
    private Integer afterQuantity;

    /**
     * 关联订单ID
     */
    private Long orderId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;


}
