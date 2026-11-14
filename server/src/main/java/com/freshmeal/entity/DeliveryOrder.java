package com.freshmeal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_delivery_order")
public class DeliveryOrder {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String deliveryNo;
    private Long orderId;
    private String orderNo;
    private Long deliveryManId;
    private String deliveryManName;
    private String deliveryManPhone;
    private Long areaId;
    private BigDecimal distance;
    private BigDecimal freight;
    private BigDecimal manFee;
    private Integer status;
    private Integer assignType;
    private LocalDateTime assignTime;
    private LocalDateTime pickupTime;
    private String pickupAddress;
    private LocalDateTime receiveTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
