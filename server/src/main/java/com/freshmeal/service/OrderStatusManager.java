package com.freshmeal.service;

import com.freshmeal.common.BusinessException;
import com.freshmeal.entity.Order;
import com.freshmeal.entity.OrderItem;
import com.freshmeal.entity.OrderStatusLog;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.enums.OrderStatus;
import com.freshmeal.mapper.OrderItemMapper;
import com.freshmeal.mapper.OrderMapper;
import com.freshmeal.mapper.OrderStatusLogMapper;
import com.freshmeal.mapper.PaymentMapper;
import com.freshmeal.mapper.ProductMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderStatusManager {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final ProductMapper productMapper;
    private final PaymentMapper paymentMapper;
    private final InventoryService inventoryService;

    @Transactional(rollbackFor = Exception.class)
    public void transfer(Order order, OrderStatus target, OrderOperator operator) {
        OrderStatus current = OrderStatus.of(order.getStatus());
        if (!current.canTransferTo(target)) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID.getCode(),
                    String.format("订单当前状态[%s]不允许变更为[%s]",
                            current.getDescription(), target.getDescription()));
        }
        if (orderMapper.transitionStatus(order.getId(), current.getCode(), target.getCode()) != 1) {
            throw new BusinessException(40904, "订单状态已变化，请刷新后重试");
        }
        order.setStatus(target.getCode());
        applySideEffect(order, target, operator);
        insertStatusLog(order, current, target, operator);
    }

    private void applySideEffect(Order order, OrderStatus target, OrderOperator operator) {
        switch (target) {
            case WAIT_ACCEPT -> markPaid(order, operator);
            case CANCELLED -> markCancelledAndRestore(order, operator);
            case FINISHED -> markFinished(order);
            default -> {
            }
        }
    }

    private void markPaid(Order order, OrderOperator operator) {
        LocalDateTime payTime = LocalDateTime.now();
        order.setPayStatus(1);
        order.setPayType(operator.payType());
        order.setPayTime(payTime);
        Order patch = new Order();
        patch.setId(order.getId());
        patch.setPayStatus(1);
        patch.setPayType(operator.payType());
        patch.setPayTime(payTime);
        orderMapper.updateById(patch);

        List<OrderItem> items = orderItemMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<OrderItem>lambdaQuery()
                        .eq(OrderItem::getOrderId, order.getId()));
        items.forEach(item -> productMapper.increaseSalesCount(
                item.getProductId(), item.getQuantity()));
    }

    private void markCancelledAndRestore(Order order, OrderOperator operator) {
        LocalDateTime cancelTime = LocalDateTime.now();
        order.setCancelTime(cancelTime);
        order.setCancelReason(operator.remark());
        order.setPayStatus(0);
        Order patch = new Order();
        patch.setId(order.getId());
        patch.setCancelTime(cancelTime);
        patch.setCancelReason(operator.remark());
        patch.setPayStatus(0);
        orderMapper.updateById(patch);
        paymentMapper.closePendingByOrder(order.getId());
        inventoryService.restoreForOrder(order.getOrderNo(), operator.operatorId(),
                operator.remark());
    }

    private void markFinished(Order order) {
        LocalDateTime finishTime = LocalDateTime.now();
        order.setFinishTime(finishTime);
        Order patch = new Order();
        patch.setId(order.getId());
        patch.setFinishTime(finishTime);
        orderMapper.updateById(patch);
    }

    private void insertStatusLog(Order order, OrderStatus source, OrderStatus target,
            OrderOperator operator) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(order.getId());
        log.setOrderNo(order.getOrderNo());
        log.setFromStatus(source.getCode());
        log.setToStatus(target.getCode());
        log.setOperatorType(operator.operatorType());
        log.setOperatorId(operator.operatorId());
        log.setOperatorName(operator.operatorName());
        log.setRemark(operator.remark());
        orderStatusLogMapper.insert(log);
    }
}
