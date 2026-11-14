package com.freshmeal.service;

import com.freshmeal.common.PageResult;
import com.freshmeal.dto.admin.AdminDeliveryQuery;
import com.freshmeal.entity.Order;
import com.freshmeal.enums.DeliveryStatus;
import com.freshmeal.vo.admin.AdminDeliveryVO;
import com.freshmeal.vo.admin.AdminRiderVO;
import com.freshmeal.vo.rider.RiderDeliveryVO;
import com.freshmeal.vo.rider.RiderStatisticsVO;
import com.freshmeal.vo.rider.RiderWorkStatusVO;
import com.freshmeal.vo.user.DeliveryTraceVO;
import java.util.List;

public interface DeliveryOrderService {

    void createForOrder(Order order);

    void syncForOrder(Order order, DeliveryStatus targetStatus);

    void cancelForOrder(Order order);

    List<RiderDeliveryVO> todo(Long riderId);

    List<RiderDeliveryVO> myTasks(Long riderId);

    RiderDeliveryVO grab(Long riderId, String deliveryNo);

    RiderDeliveryVO pickup(Long riderId, String deliveryNo);

    RiderDeliveryVO deliver(Long riderId, String deliveryNo);

    RiderStatisticsVO statistics(Long riderId);

    RiderWorkStatusVO updateWorkStatus(
            Long riderId, Integer onlineStatus, Integer workStatus);

    PageResult<AdminDeliveryVO> adminPage(AdminDeliveryQuery query);

    AdminDeliveryVO adminAssign(Long deliveryId, Long riderId);

    List<AdminRiderVO> adminRiders();

    List<DeliveryTraceVO> traceForUser(Long userId, String orderNo);

    void dispatchWaitingOrders();
}
