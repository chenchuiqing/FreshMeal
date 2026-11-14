package com.freshmeal.dto.admin;

import com.freshmeal.dto.common.PageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminRiderQuery extends PageQuery {

    private String keyword;
    private Integer status;
    private Integer onlineStatus;
}
