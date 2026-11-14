package com.freshmeal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_product_batch")
public class ProductBatch {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String batchNo;
    private LocalDate productionDate;
    private LocalDate expireDate;
    private Integer stock;
    private Integer lockStock;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
