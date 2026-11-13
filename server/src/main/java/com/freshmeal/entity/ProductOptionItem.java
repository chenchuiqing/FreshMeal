package com.freshmeal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_product_option_item")
public class ProductOptionItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long optionId;

    private String name;

    private BigDecimal priceDelta;

    private Integer calories;

    private Integer sort;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
