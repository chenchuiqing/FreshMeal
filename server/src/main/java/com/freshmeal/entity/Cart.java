package com.freshmeal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_cart")
public class Cart {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long productId;
    private Long specId;
    private Integer quantity;
    private String options;
    private Integer checked;
    private String productName;
    private String cover;
    private BigDecimal price;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
