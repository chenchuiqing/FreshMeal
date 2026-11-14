package com.freshmeal.dto.admin;

import com.freshmeal.dto.common.PageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminBatchQuery extends PageQuery {

    private Long productId;
    private String batchNo;
    private Integer status;
}
