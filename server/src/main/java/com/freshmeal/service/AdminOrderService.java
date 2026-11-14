package com.freshmeal.service;

import com.freshmeal.common.PageResult;
import com.freshmeal.dto.admin.AdminOrderQuery;
import com.freshmeal.vo.admin.AdminOrderDetailVO;
import com.freshmeal.vo.admin.AdminOrderVO;

public interface AdminOrderService {

    PageResult<AdminOrderVO> page(AdminOrderQuery query);

    AdminOrderDetailVO detail(String orderNo);

    AdminOrderVO updateStatus(String orderNo, Integer targetStatus, String reason, Long adminId);
}
