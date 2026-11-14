package com.freshmeal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_delivery_area")
public class DeliveryArea {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String city;
    private String district;
    private BigDecimal baseFee;
    private BigDecimal perKmFee;
    private BigDecimal freeThreshold;
    private BigDecimal nightSurcharge;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
