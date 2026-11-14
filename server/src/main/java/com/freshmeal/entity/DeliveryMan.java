package com.freshmeal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_delivery_man")
public class DeliveryMan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String realName;

    private String phone;

    private String avatar;

    private Integer workStatus;

    private Integer onlineStatus;

    private Long serviceAreaId;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private Integer maxOrderNum;

    private Integer todayOrderCount;

    private Integer totalOrderCount;

    private BigDecimal totalIncome;

    private BigDecimal rating;

    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
