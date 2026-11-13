package com.freshmeal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_product_option")
public class ProductOption {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private String name;

    private Integer type;

    private Integer required;

    private Integer sort;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
