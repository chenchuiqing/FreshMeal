package com.freshmeal.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.freshmeal.common.BusinessException;
import com.freshmeal.dto.user.MockPayNotifyRequest;
import com.freshmeal.entity.Order;
import com.freshmeal.entity.Payment;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.enums.OrderStatus;
import com.freshmeal.mapper.OrderMapper;
import com.freshmeal.mapper.PaymentMapper;
import com.freshmeal.service.OrderOperator;
import com.freshmeal.service.OrderStatusManager;
import com.freshmeal.service.PaymentService;
import com.freshmeal.vo.user.PaymentVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final int PAYMENT_PENDING = 0;
    private static final int PAYMENT_SUCCESS = 1;
    private static final int PAYMENT_FAILED = 2;
    private static final int PAYMENT_CLOSED = 3;
    private static final List<Integer> MOCK_PAY_TYPES = List.of(1, 2);
    private static final DateTimeFormatter PAY_NO_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT);

    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final OrderStatusManager orderStatusManager;
    private final ObjectMapper objectMapper;

    @Value("${freshmeal.order.pay-expire-minutes:15}")
    private int payExpireMinutes;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO initiate(Long userId, String orderNo, Integer payType) {
        if (payType == null || !MOCK_PAY_TYPES.contains(payType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "支付方式不合法");
        }
        Order order = orderMapper.selectByOrderNoForUpdate(orderNo);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND.getCode(),
                    ErrorCode.ORDER_NOT_FOUND.getMessage());
        }
        if (order.getStatus() != OrderStatus.WAIT_PAY.getCode()
                || order.getPayStatus() != 0) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID.getCode(),
                    ErrorCode.ORDER_STATE_INVALID.getMessage());
        }

        List<Payment> payments = paymentMapper.selectList(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getOrderId, order.getId())
                .orderByDesc(Payment::getId));
        for (Payment payment : payments) {
            if (payment.getStatus() == PAYMENT_SUCCESS) {
                return toVo(payment);
            }
            if (payment.getStatus() == PAYMENT_PENDING) {
                if (payment.getPayType() != payType) {
                    paymentMapper.closePendingByOrder(order.getId());
                    continue;
                }
                if (payment.getExpireTime() != null
                        && payment.getExpireTime().isBefore(LocalDateTime.now())) {
                    throw new BusinessException(ErrorCode.PAYMENT_EXPIRED.getCode(),
                            ErrorCode.PAYMENT_EXPIRED.getMessage());
                }
                return toVo(payment);
            }
        }

        Payment payment = new Payment();
        payment.setPayNo(generatePayNo());
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setUserId(userId);
        payment.setPayType(payType);
        payment.setAmount(order.getPayAmount());
        payment.setStatus(PAYMENT_PENDING);
        payment.setExpireTime(LocalDateTime.now().plusMinutes(payExpireMinutes));
        paymentMapper.insert(payment);
        return toVo(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO mockNotify(Long userId, MockPayNotifyRequest request) {
        Payment pendingLookup = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getPayNo, request.payNo())
                .last("LIMIT 1"));
        if (pendingLookup == null || !userId.equals(pendingLookup.getUserId())) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND.getCode(),
                    ErrorCode.PAYMENT_NOT_FOUND.getMessage());
        }
        if (pendingLookup.getStatus() == PAYMENT_SUCCESS) {
            return toVo(pendingLookup);
        }

        Order order = orderMapper.selectByOrderNoForUpdate(pendingLookup.getOrderNo());
        Payment payment = paymentMapper.selectByPayNoForUpdate(request.payNo());
        if (order == null || payment == null || !order.getId().equals(payment.getOrderId())) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND.getCode(),
                    ErrorCode.PAYMENT_NOT_FOUND.getMessage());
        }
        if (payment.getStatus() == PAYMENT_SUCCESS) {
            return toVo(payment);
        }
        if (payment.getStatus() != PAYMENT_PENDING) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID.getCode(),
                    ErrorCode.ORDER_STATE_INVALID.getMessage());
        }
        if (!MOCK_PAY_TYPES.contains(request.payType())
                || request.payType() != payment.getPayType()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "支付方式不匹配");
        }

        if (!Boolean.TRUE.equals(request.success())) {
            payment.setStatus(PAYMENT_FAILED);
            payment.setNotifyContent(writeNotify(request));
            paymentMapper.updateById(payment);
            return toVo(payment);
        }
        if (payment.getExpireTime() != null
                && payment.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.PAYMENT_EXPIRED.getCode(),
                    ErrorCode.PAYMENT_EXPIRED.getMessage());
        }
        if (order.getStatus() != OrderStatus.WAIT_PAY.getCode()) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID.getCode(),
                    ErrorCode.ORDER_STATE_INVALID.getMessage());
        }

        payment.setStatus(PAYMENT_SUCCESS);
        payment.setThirdPartyNo(request.thirdPartyNo() == null
                ? "MOCK-" + payment.getPayNo()
                : request.thirdPartyNo());
        payment.setNotifyContent(writeNotify(request));
        payment.setPayTime(LocalDateTime.now());
        paymentMapper.updateById(payment);

        orderStatusManager.transfer(order, OrderStatus.WAIT_ACCEPT,
                new OrderOperator(0, null, "模拟支付回调", "模拟支付成功", payment.getPayType()));
        return toVo(payment);
    }

    private PaymentVO toVo(Payment payment) {
        return new PaymentVO(
                payment.getId(),
                payment.getPayNo(),
                payment.getOrderNo(),
                payment.getPayType(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getExpireTime(),
                payment.getPayTime(),
                "/pay/" + payment.getOrderNo()
        );
    }

    private String generatePayNo() {
        String time = LocalDateTime.now().format(PAY_NO_TIME);
        String random = String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        return "PAY" + time + random;
    }

    private String writeNotify(MockPayNotifyRequest request) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "payNo", request.payNo(),
                    "payType", request.payType(),
                    "success", request.success(),
                    "thirdPartyNo", request.thirdPartyNo() == null ? "" : request.thirdPartyNo()
            ));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(),
                    ErrorCode.INTERNAL_ERROR.getMessage());
        }
    }
}
