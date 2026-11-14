package com.freshmeal.dto.admin;

import com.freshmeal.dto.common.PageQuery;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminOrderQuery extends PageQuery {

    private String orderNo;
    private String username;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
