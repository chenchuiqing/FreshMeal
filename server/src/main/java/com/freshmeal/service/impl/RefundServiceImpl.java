package com.freshmeal.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freshmeal.common.BusinessException;
import com.freshmeal.common.PageResult;
import com.freshmeal.entity.Order;
import com.freshmeal.entity.Refund;
import com.freshmeal.entity.User;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.enums.OrderStatus;
import com.freshmeal.mapper.OrderMapper;
import com.freshmeal.mapper.RefundMapper;
import com.freshmeal.mapper.UserMapper;
import com.freshmeal.service.OrderOperator;
import com.freshmeal.service.OrderStatusManager;
import com.freshmeal.service.RefundService;
import com.freshmeal.vo.admin.AdminRefundVO;
import com.freshmeal.vo.user.RefundVO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private static final int REFUND_PENDING = 0;
    private static final int REFUND_APPROVED = 1;
    private static final int REFUND_REJECTED = 2;

    private final RefundMapper refundMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final OrderStatusManager orderStatusManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void request(Long userId, String orderNo, String reason) {
        Order order = orderMapper.selectByOrderNoForUpdate(orderNo);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND.getCode(),
                    ErrorCode.ORDER_NOT_FOUND.getMessage());
        }
        if (order.getPayStatus() == null || order.getPayStatus() != 1
                || order.getStatus() < OrderStatus.WAIT_ACCEPT.getCode()
                || order.getStatus() > OrderStatus.FINISHED.getCode()) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID.getCode(),
                    "当前订单不支持申请售后退款");
        }
        Long exists = refundMapper.selectCount(Wrappers.<Refund>lambdaQuery()
                .eq(Refund::getOrderId, order.getId()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(),
                    "该订单已提交售后申请");
        }
        Refund refund = new Refund();
        refund.setOrderId(order.getId());
        refund.setOrderNo(order.getOrderNo());
        refund.setUserId(userId);
        refund.setAmount(order.getPayAmount() == null
                ? BigDecimal.ZERO : order.getPayAmount());
        refund.setReason(reason.trim());
        refund.setStatus(REFUND_PENDING);
        refund.setPreviousStatus(order.getStatus());
        refundMapper.insert(refund);
        orderStatusManager.transfer(order, OrderStatus.REFUNDING,
                OrderOperator.user(userId, "申请售后退款"));
    }

    @Override
    public List<RefundVO> listUser(Long userId) {
        return refundMapper.selectList(Wrappers.<Refund>lambdaQuery()
                        .eq(Refund::getUserId, userId)
                        .orderByDesc(Refund::getCreateTime))
                .stream()
                .map(this::toUserVo)
                .toList();
    }

    @Override
    public PageResult<AdminRefundVO> pageAdmin(int current, int size, Integer status) {
        Page<Refund> page = refundMapper.selectPage(
                new Page<>(Math.max(current, 1), Math.max(size, 1)),
                Wrappers.<Refund>lambdaQuery()
                        .eq(status != null, Refund::getStatus, status)
                        .orderByDesc(Refund::getCreateTime)
                        .orderByDesc(Refund::getId));
        Map<Long, User> users = loadUsers(page.getRecords());
        List<AdminRefundVO> records = page.getRecords().stream()
                .map(refund -> {
                    User user = users.get(refund.getUserId());
                    return new AdminRefundVO(
                            refund.getId(),
                            refund.getOrderId(),
                            refund.getOrderNo(),
                            refund.getUserId(),
                            user == null ? null : user.getUsername(),
                            user == null ? null : user.getNickname(),
                            refund.getAmount(),
                            refund.getReason(),
                            refund.getStatus(),
                            refund.getPreviousStatus(),
                            refund.getAdminReply(),
                            refund.getCreateTime(),
                            refund.getUpdateTime());
                })
                .toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decide(Long id, Integer status, String adminReply, Long adminId) {
        if (status == null || (status != REFUND_APPROVED && status != REFUND_REJECTED)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "退款处理结果不合法");
        }
        Refund refund = refundMapper.selectById(id);
        if (refund == null) {
            throw new BusinessException(ErrorCode.REFUND_NOT_FOUND.getCode(),
                    ErrorCode.REFUND_NOT_FOUND.getMessage());
        }
        if (refund.getStatus() != REFUND_PENDING) {
            throw new BusinessException(ErrorCode.REFUND_STATE_INVALID.getCode(),
                    ErrorCode.REFUND_STATE_INVALID.getMessage());
        }
        Order order = orderMapper.selectByOrderNoForUpdate(refund.getOrderNo());
        if (order == null || order.getStatus() != OrderStatus.REFUNDING.getCode()) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID.getCode(),
                    ErrorCode.ORDER_STATE_INVALID.getMessage());
        }
        if (status == REFUND_APPROVED) {
            orderStatusManager.transfer(order, OrderStatus.REFUNDED,
                    OrderOperator.merchant(adminId, "管理员",
                            StringUtils.hasText(adminReply)
                                    ? adminReply.trim() : "售后退款通过"));
        } else {
            OrderStatus previous = OrderStatus.of(refund.getPreviousStatus());
            orderStatusManager.transfer(order, previous,
                    OrderOperator.merchant(adminId, "管理员",
                            StringUtils.hasText(adminReply)
                                    ? adminReply.trim() : "售后申请被驳回"));
        }
        refund.setStatus(status);
        refund.setAdminReply(StringUtils.hasText(adminReply)
                ? adminReply.trim() : null);
        refundMapper.updateById(refund);
    }

    private Map<Long, User> loadUsers(List<Refund> refunds) {
        List<Long> ids = refunds.stream()
                .map(Refund::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(),
                        (first, second) -> first));
    }

    private RefundVO toUserVo(Refund refund) {
        return new RefundVO(
                refund.getId(),
                refund.getOrderNo(),
                refund.getAmount(),
                refund.getReason(),
                refund.getStatus(),
                refund.getPreviousStatus(),
                refund.getAdminReply(),
                refund.getCreateTime(),
                refund.getUpdateTime());
    }
}
