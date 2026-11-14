package com.freshmeal.dto.user;

import com.freshmeal.dto.common.PageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderQuery extends PageQuery {

    private Integer status;

    private String orderNo;
}
