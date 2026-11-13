package com.freshmeal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String name;

    private String subtitle;

    private String cover;

    private String detail;

    private Integer productType;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private String unit;

    private Integer stock;

    private Integer salesCount;

    private Integer status;

    private Integer isRecommend;

    private Integer sort;

    private Integer version;

    private Integer calories;

    private BigDecimal protein;

    private BigDecimal fat;

    private BigDecimal carbohydrate;

    private BigDecimal sodium;

    private String nutritionDesc;

    private String allergens;

    private Integer storageType;

    private Integer shelfLifeDays;

    private String producer;

    private String heatingMethod;

    private String healthTags;

    private Long defaultSpecId;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
