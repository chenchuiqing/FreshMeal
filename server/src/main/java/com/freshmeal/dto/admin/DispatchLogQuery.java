package com.freshmeal.dto.admin;

import com.freshmeal.dto.common.PageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DispatchLogQuery extends PageQuery {

    private Long deliveryOrderId;
    private String deliveryNo;
    private Long candidateId;
    private Integer isFinal;
}
