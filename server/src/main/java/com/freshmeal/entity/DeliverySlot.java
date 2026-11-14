package com.freshmeal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalTime;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_delivery_slot")
public class DeliverySlot {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalTime orderDeadline;
    private Integer dailyCapacity;
    private Integer advanceDays;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
