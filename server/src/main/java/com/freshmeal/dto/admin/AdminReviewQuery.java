package com.freshmeal.dto.admin;

import com.freshmeal.dto.common.PageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminReviewQuery extends PageQuery {

    private String orderNo;
    private Integer rating;
}
