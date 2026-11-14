package com.freshmeal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_delivery_trace")
public class DeliveryTrace {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deliveryOrderId;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String address;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
}
