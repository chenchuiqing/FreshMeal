package com.freshmeal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_payment")
public class Payment {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String payNo;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private Integer payType;
    private BigDecimal amount;
    private Integer status;
    private String thirdPartyNo;
    private String notifyContent;
    private LocalDateTime payTime;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
