package com.freshmeal.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freshmeal.common.BusinessException;
import com.freshmeal.common.PageResult;
import com.freshmeal.dto.admin.AdminOrderQuery;
import com.freshmeal.entity.Order;
import com.freshmeal.entity.OrderItem;
import com.freshmeal.entity.OrderStatusLog;
import com.freshmeal.entity.User;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.enums.OrderStatus;
import com.freshmeal.mapper.OrderItemMapper;
import com.freshmeal.mapper.OrderMapper;
import com.freshmeal.mapper.OrderStatusLogMapper;
import com.freshmeal.mapper.UserMapper;
import com.freshmeal.service.AdminOrderService;
import com.freshmeal.service.OrderOperator;
import com.freshmeal.service.OrderStatusManager;
import com.freshmeal.vo.admin.AdminOrderDetailVO;
import com.freshmeal.vo.admin.AdminOrderItemVO;
import com.freshmeal.vo.admin.AdminOrderTimelineVO;
import com.freshmeal.vo.admin.AdminOrderVO;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final UserMapper userMapper;
    private final OrderStatusManager orderStatusManager;

    @Override
    public PageResult<AdminOrderVO> page(AdminOrderQuery query) {
        List<Long> userIds = findUserIds(query.getUsername());
        if (query.getUsername() != null && !query.getUsername().isBlank()
                && userIds.isEmpty()) {
            return PageResult.of(List.of(), 0, query.getCurrent(), query.getSize());
        }
        Page<Order> page = orderMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                Wrappers.<Order>lambdaQuery()
                        .in(userIds != null && !userIds.isEmpty(),
                                Order::getUserId, userIds)
                        .eq(query.getStatus() != null,
                                Order::getStatus, query.getStatus())
                        .like(query.getOrderNo() != null && !query.getOrderNo().isBlank(),
                                Order::getOrderNo, query.getOrderNo().trim())
                        .ge(query.getStartTime() != null,
                                Order::getCreateTime, query.getStartTime())
                        .le(query.getEndTime() != null,
                                Order::getCreateTime, query.getEndTime())
                        .orderByDesc(Order::getCreateTime)
                        .orderByDesc(Order::getId));
        Map<Long, User> users = loadUsers(page.getRecords());
        List<AdminOrderVO> records = page.getRecords().stream()
                .map(order -> toVo(order, users.get(order.getUserId())))
                .toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public AdminOrderDetailVO detail(String orderNo) {
        Order order = requireOrder(orderNo);
        User user = userMapper.selectById(order.getUserId());
        List<AdminOrderItemVO> items = orderItemMapper.selectList(
                        Wrappers.<OrderItem>lambdaQuery()
                                .eq(OrderItem::getOrderId, order.getId())
                                .orderByAsc(OrderItem::getId))
                .stream()
                .map(this::toItemVo)
                .toList();
        List<AdminOrderTimelineVO> timeline = orderStatusLogMapper.selectList(
                        Wrappers.<OrderStatusLog>lambdaQuery()
                                .eq(OrderStatusLog::getOrderId, order.getId())
                                .orderByAsc(OrderStatusLog::getCreateTime)
                                .orderByAsc(OrderStatusLog::getId))
                .stream()
                .map(this::toTimelineVo)
                .toList();
        return new AdminOrderDetailVO(toVo(order, user), items, timeline);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminOrderVO updateStatus(
            String orderNo, Integer targetStatus, String reason, Long adminId) {
        Order order = orderMapper.selectByOrderNoForUpdate(orderNo);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND.getCode(),
                    ErrorCode.ORDER_NOT_FOUND.getMessage());
        }
        String remark = reason == null || reason.isBlank()
                ? "管理员状态干预"
                : reason.trim();
        orderStatusManager.transfer(order, OrderStatus.of(targetStatus),
                OrderOperator.merchant(adminId, "管理员", remark));
        User user = userMapper.selectById(order.getUserId());
        return toVo(order, user);
    }

    private List<Long> findUserIds(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        String keyword = username.trim();
        return userMapper.selectList(Wrappers.<User>lambdaQuery()
                        .like(User::getUsername, keyword)
                        .or().like(User::getNickname, keyword)
                        .or().like(User::getPhone, keyword))
                .stream()
                .map(User::getId)
                .toList();
    }

    private Map<Long, User> loadUsers(List<Order> orders) {
        List<Long> userIds = orders.stream()
                .map(Order::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Order requireOrder(String orderNo) {
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND.getCode(),
                    ErrorCode.ORDER_NOT_FOUND.getMessage());
        }
        return order;
    }

    private AdminOrderVO toVo(Order order, User user) {
        return new AdminOrderVO(
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getNickname(),
                user == null ? null : user.getPhone(),
                order.getStatus(),
                OrderStatus.of(order.getStatus()).getDescription(),
                order.getPayStatus(),
                order.getPayType(),
                order.getDeliveryType(),
                order.getGoodsAmount(),
                order.getFreightAmount(),
                order.getDiscountAmount(),
                order.getPayAmount(),
                order.getReceiver(),
                order.getReceiverPhone(),
                order.getReceiverAddress(),
                order.getDeliverySlotName(),
                order.getDeliverySlotDate(),
                order.getCreateTime(),
                order.getPayTime(),
                order.getFinishTime(),
                order.getCancelTime(),
                order.getCancelReason()
        );
    }

    private AdminOrderItemVO toItemVo(OrderItem item) {
        return new AdminOrderItemVO(
                item.getProductId(),
                item.getProductName(),
                item.getProductCover(),
                item.getSpecName(),
                item.getOptionsDesc(),
                item.getPrice(),
                item.getQuantity(),
                item.getSubtotal(),
                item.getCalories()
        );
    }

    private AdminOrderTimelineVO toTimelineVo(OrderStatusLog log) {
        return new AdminOrderTimelineVO(
                log.getFromStatus(),
                log.getToStatus(),
                OrderStatus.of(log.getToStatus()).getDescription(),
                log.getOperatorName(),
                log.getRemark(),
                log.getCreateTime()
        );
    }
}
