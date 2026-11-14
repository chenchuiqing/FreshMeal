package com.freshmeal.vo.admin;

import java.util.List;

public record AdminOrderDetailVO(
        AdminOrderVO order,
        List<AdminOrderItemVO> items,
        List<AdminOrderTimelineVO> timeline
) {
}
