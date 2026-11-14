package com.freshmeal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private Long productId;
    private String productName;
    private String productCover;
    private Long specId;
    private String specName;
    private String options;
    private String optionsDesc;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
    private Integer calories;
    private LocalDateTime createTime;
}
