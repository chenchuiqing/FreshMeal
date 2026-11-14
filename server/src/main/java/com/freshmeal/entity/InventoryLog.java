package com.freshmeal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_inventory_log")
public class InventoryLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Long batchId;
    private Integer changeType;
    private Integer changeNum;
    private Integer beforeStock;
    private Integer afterStock;
    private String refType;
    private String refNo;
    private Long operatorId;
    private String remark;
    private LocalDateTime createTime;
}
