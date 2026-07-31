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
 * 库存表
 * </p>
 *
 * @author zhlyu
 * @since 2026-07-31
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("inventory")
public class Inventory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 库存记录ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 总库存数量
     */
    private Integer quantity;

    /**
     * 已预留数量
     */
    private Integer reservedQuantity;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;


}
