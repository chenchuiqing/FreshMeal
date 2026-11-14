package com.freshmeal.dto.admin;

import com.freshmeal.dto.common.PageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDeliveryQuery extends PageQuery {

    private Integer status;
    private String orderNo;
}
