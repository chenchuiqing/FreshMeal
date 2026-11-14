package com.freshmeal.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.freshmeal.entity.Order;
import com.freshmeal.entity.OrderItem;
import com.freshmeal.entity.User;
import com.freshmeal.enums.OrderStatus;
import com.freshmeal.mapper.OrderItemMapper;
import com.freshmeal.mapper.OrderMapper;
import com.freshmeal.mapper.UserMapper;
import com.freshmeal.service.AdminReportService;
import com.freshmeal.vo.admin.OrderReportRow;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;

    @Override
    public List<OrderReportRow> orderRows(String startTime, String endTime) {
        List<Order> orders = orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .ge(StringUtils.hasText(startTime), Order::getCreateTime,
                        parse(startTime))
                .le(StringUtils.hasText(endTime), Order::getCreateTime,
                        parse(endTime))
                .orderByAsc(Order::getCreateTime)
                .orderByAsc(Order::getId));
        if (orders.isEmpty()) {
            return List.of();
        }
        Map<Long, User> users = userMapper.selectBatchIds(orders.stream()
                        .map(Order::getUserId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity(),
                        (first, second) -> first));
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> items = orderItemMapper.selectList(
                        Wrappers.<OrderItem>lambdaQuery()
                                .in(OrderItem::getOrderId, orderIds)
                                .orderByAsc(OrderItem::getId))
                .stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        return orders.stream()
                .flatMap(order -> items.getOrDefault(order.getId(), List.of()).stream()
                        .map(item -> new OrderReportRow(
                                order.getOrderNo(),
                                users.get(order.getUserId()) == null
                                        ? null : users.get(order.getUserId()).getUsername(),
                                OrderStatus.of(order.getStatus()).getDescription(),
                                order.getCreateTime(),
                                item.getProductName(),
                                item.getSpecName(),
                                item.getQuantity(),
                                item.getSubtotal(),
                                order.getPayAmount())))
                .toList();
    }

    private LocalDateTime parse(String value) {
        return value == null ? null : LocalDateTime.parse(value);
    }
}
