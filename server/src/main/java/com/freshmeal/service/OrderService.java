package com.freshmeal.service;

import com.freshmeal.common.PageResult;
import com.freshmeal.dto.user.OrderCreateRequest;
import com.freshmeal.dto.user.OrderQuery;
import com.freshmeal.dto.user.OrderSettleRequest;
import com.freshmeal.vo.user.OrderSettleVO;
import com.freshmeal.vo.user.OrderVO;

public interface OrderService {

    OrderSettleVO settle(Long userId, OrderSettleRequest request);

    OrderVO create(Long userId, OrderCreateRequest request);

    PageResult<OrderVO> page(Long userId, OrderQuery query);

    OrderVO detail(Long userId, String orderNo);

    void cancel(Long userId, String orderNo, String reason);

    void cancelExpiredUnpaidOrders();

    void advanceReadyOrders();

    void activateDueSlotOrders();
}
