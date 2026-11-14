package com.freshmeal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal goodsAmount;
    private BigDecimal freightAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private BigDecimal deliveryManFee;
    private String receiver;
    private String receiverPhone;
    private String receiverAddress;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer deliveryType;
    private LocalDateTime expectTime;
    private Long deliverySlotId;
    private LocalDate deliverySlotDate;
    private String deliverySlotName;
    private Long deliveryManId;
    private Long deliveryAreaId;
    private BigDecimal deliveryDistance;
    private Integer status;
    private Integer payStatus;
    private Integer payType;
    private LocalDateTime payTime;
    private String remark;
    private String cancelReason;
    private LocalDateTime cancelTime;
    private LocalDateTime finishTime;
    private Long userCouponId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
