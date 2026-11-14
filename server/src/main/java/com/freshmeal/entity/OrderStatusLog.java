package com.freshmeal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_order_status_log")
public class OrderStatusLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String orderNo;
    private Integer fromStatus;
    private Integer toStatus;
    private Integer operatorType;
    private Long operatorId;
    private String operatorName;
    private String remark;
    private LocalDateTime createTime;
}
