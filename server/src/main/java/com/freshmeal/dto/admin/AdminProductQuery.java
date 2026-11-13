package com.freshmeal.dto.admin;

import com.freshmeal.dto.common.PageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminProductQuery extends PageQuery {

    private String name;

    private Long categoryId;

    private Integer status;

    private Integer productType;
}
