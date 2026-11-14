package com.freshmeal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_delivery_man_log")
public class DeliveryManLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deliveryOrderId;
    private Long candidateId;
    private String candidateName;
    private BigDecimal totalScore;
    private BigDecimal distanceScore;
    private BigDecimal loadScore;
    private BigDecimal ratingScore;
    private BigDecimal distanceValue;
    private Integer loadValue;
    private Integer isFinal;
    private LocalDateTime createTime;
}
