# 附录 A 核心源码

本附录收录系统后端的核心源码，文件完整路径为 `server/src/main/java/com/freshmeal/`，为突出主线省略了导入语句、查询与 VO 组装等辅助方法，完整代码见发布包。

## A.1 订单状态枚举（enums/OrderStatus.java）

```java
package com.freshmeal.enums;

import java.util.Arrays;
import java.util.Set;

public enum OrderStatus {

    WAIT_PAY(0, "待支付", Set.of(1, 9)),
    WAIT_ACCEPT(1, "待接单", Set.of(2, 7, 9)),
    PREPARING(2, "备餐中", Set.of(3, 4, 7)),
    WAIT_SLOT(3, "待配送时段", Set.of(4, 7)),
    WAIT_DELIVER(4, "待配送", Set.of(5, 7, 9)),
    DELIVERING(5, "配送中", Set.of(6, 7)),
    FINISHED(6, "已完成", Set.of(7)),
    REFUNDING(7, "退款中", Set.of(8, 1, 2, 3, 4, 5, 6)),
    REFUNDED(8, "已退款", Set.of()),
    CANCELLED(9, "已取消", Set.of());

    private final int code;
    private final String description;
    private final Set<Integer> nextCodes;

    OrderStatus(int code, String description, Set<Integer> nextCodes) {
        this.code = code;
        this.description = description;
        this.nextCodes = nextCodes;
    }

    public static OrderStatus of(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("订单状态不能为空");
        }
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知订单状态: " + code));
    }

    public boolean canTransferTo(OrderStatus target) {
        return nextCodes.contains(target.code);
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
```

## A.2 订单状态机（service/OrderStatusManager.java）

```java
package com.freshmeal.service;

import com.freshmeal.common.BusinessException;
import com.freshmeal.entity.Order;
import com.freshmeal.entity.OrderItem;
import com.freshmeal.entity.OrderStatusLog;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.enums.DeliveryStatus;
import com.freshmeal.enums.OrderStatus;
import com.freshmeal.mapper.OrderItemMapper;
import com.freshmeal.mapper.OrderMapper;
import com.freshmeal.mapper.OrderStatusLogMapper;
import com.freshmeal.mapper.PaymentMapper;
import com.freshmeal.mapper.ProductMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
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
    private final CouponService couponService;
    private final ObjectProvider<DeliveryOrderService> deliveryOrderServiceProvider;

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
            case CANCELLED -> {
                markCancelledAndRestore(order, operator);
                cancelDeliveryOrder(order);
            }
            case REFUNDED -> markRefunded(order, operator);
            case FINISHED -> {
                markFinished(order);
                syncDeliveryStatus(order, DeliveryStatus.DELIVERED);
            }
            case WAIT_DELIVER -> createDeliveryOrder(order);
            case DELIVERING -> syncDeliveryStatus(order, DeliveryStatus.DELIVERING);
            default -> {
            }
        }
    }

    private void createDeliveryOrder(Order order) {
        deliveryOrderServiceProvider.ifAvailable(service -> service.createForOrder(order));
    }

    private void syncDeliveryStatus(Order order, DeliveryStatus status) {
        deliveryOrderServiceProvider.ifAvailable(
                service -> service.syncForOrder(order, status));
    }

    private void cancelDeliveryOrder(Order order) {
        deliveryOrderServiceProvider.ifAvailable(service -> service.cancelForOrder(order));
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
        couponService.release(order.getUserCouponId());
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

    private void markRefunded(Order order, OrderOperator operator) {
        order.setPayStatus(2);
        Order patch = new Order();
        patch.setId(order.getId());
        patch.setPayStatus(2);
        orderMapper.updateById(patch);
        paymentMapper.update(null,
                com.baomidou.mybatisplus.core.toolkit.Wrappers
                        .<com.freshmeal.entity.Payment>lambdaUpdate()
                        .eq(com.freshmeal.entity.Payment::getOrderId, order.getId())
                        .set(com.freshmeal.entity.Payment::getStatus, 2));
        couponService.release(order.getUserCouponId());
        inventoryService.restoreForOrder(order.getOrderNo(), operator.operatorId(),
                operator.remark());
        cancelDeliveryOrder(order);
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
```

## A.3 统一响应（common/Result.java）

```java
package com.freshmeal.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.freshmeal.enums.ErrorCode;

@JsonInclude(JsonInclude.Include.ALWAYS)
public final class Result<T> {

    private final int code;
    private final String message;
    private final T data;
    private final long timestamp;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
```

## A.4 全局异常处理（common/GlobalExceptionHandler.java）

```java
package com.freshmeal.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.freshmeal.enums.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException exception) {
        HttpStatus status = resolveStatus(exception.getCode());
        LOGGER.warn("Business exception: code={}, message={}", exception.getCode(), exception.getMessage());
        return ResponseEntity.status(status)
                .body(Result.error(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Result<Void>> handleNotLoginException(NotLoginException exception) {
        LOGGER.debug("Sa-Token login exception", exception);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.error(ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMessage()));
    }

    @ExceptionHandler({NotRoleException.class, NotPermissionException.class})
    public ResponseEntity<Result<Void>> handleAuthorizationException(Exception exception) {
        LOGGER.debug("Sa-Token authorization exception", exception);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.error(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<Result<Void>> handleBadRequest(Exception exception) {
        return ResponseEntity.badRequest()
                .body(Result.error(ErrorCode.BAD_REQUEST.getCode(), resolveBadRequestMessage(exception)));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.error(ErrorCode.BAD_REQUEST.getCode(), "请求方法不支持"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(Result.error(ErrorCode.BAD_REQUEST.getCode(), "不支持的请求内容类型"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        return ResponseEntity.badRequest()
                .body(Result.error(ErrorCode.UPLOAD_SIZE_EXCEEDED.getCode(), ErrorCode.UPLOAD_SIZE_EXCEEDED.getMessage()));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Result<Void>> handleMultipartException(MultipartException exception) {
        LOGGER.warn("Multipart parse failure", exception);
        return ResponseEntity.badRequest()
                .body(Result.error(ErrorCode.BAD_REQUEST.getCode(), "上传文件解析失败"));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<Result<Void>> handleNotFound(Exception exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.error(ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpectedException(Exception exception) {
        LOGGER.error("Unhandled exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage()));
    }

    private HttpStatus resolveStatus(int code) {
        if (code >= 40000 && code < 40100) {
            return HttpStatus.BAD_REQUEST;
        }
        if (code >= 40100 && code < 40200) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code >= 40300 && code < 40400) {
            return HttpStatus.FORBIDDEN;
        }
        if (code >= 40400 && code < 40500) {
            return HttpStatus.NOT_FOUND;
        }
        if (code >= 40900 && code < 41000) {
            return HttpStatus.CONFLICT;
        }
        if (code >= 50000 && code < 60000) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveBadRequestMessage(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException validationException) {
            return resolveFieldErrorMessage(validationException.getBindingResult().getFieldError());
        }
        if (exception instanceof BindException bindException) {
            return resolveFieldErrorMessage(bindException.getBindingResult().getFieldError());
        }
        if (exception instanceof ConstraintViolationException violationException) {
            return violationException.getConstraintViolations().stream()
                    .findFirst()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .orElse(ErrorCode.BAD_REQUEST.getMessage());
        }
        if (exception instanceof HttpMessageNotReadableException) {
            return "请求体格式错误";
        }
        if (exception instanceof MethodArgumentTypeMismatchException mismatchException) {
            return mismatchException.getName() + "参数类型错误";
        }
        if (exception instanceof MissingServletRequestParameterException missingException) {
            return missingException.getParameterName() + "参数不能为空";
        }
        return ErrorCode.BAD_REQUEST.getMessage();
    }

    private String resolveFieldErrorMessage(FieldError fieldError) {
        if (fieldError == null || fieldError.getDefaultMessage() == null) {
            return ErrorCode.BAD_REQUEST.getMessage();
        }
        if (fieldError.getField() == null || fieldError.getField().isBlank()) {
            return fieldError.getDefaultMessage();
        }
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
```

## A.5 运费计算（service/impl/FreightRuleCalculator.java）

```java
package com.freshmeal.service.impl;

import com.freshmeal.entity.DeliveryArea;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class FreightRuleCalculator {

    private static final BigDecimal FIRST_KM = BigDecimal.ONE;

    public BigDecimal calculate(DeliveryArea area, BigDecimal distance,
            BigDecimal goodsAmount, boolean nightTime) {
        BigDecimal safeDistance = distance == null ? BigDecimal.ZERO : distance;
        BigDecimal freight = area.getBaseFee();
        BigDecimal extraDistance = safeDistance.subtract(FIRST_KM).max(BigDecimal.ZERO);
        freight = freight.add(extraDistance.multiply(area.getPerKmFee()));
        if (nightTime) {
            freight = freight.add(area.getNightSurcharge());
        }
        if (area.getFreeThreshold().compareTo(BigDecimal.ZERO) > 0
                && goodsAmount.compareTo(area.getFreeThreshold()) >= 0) {
            freight = BigDecimal.ZERO;
        }
        return freight.setScale(2, RoundingMode.HALF_UP);
    }
}
```

## A.6 支付服务（service/impl/PaymentServiceImpl.java）

发起支付与幂等回调处理。

```java
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
```

## A.7 库存服务（service/impl/InventoryServiceImpl.java）

乐观锁扣减、FEFO 批次出库与按原批次回补。

```java
package com.freshmeal.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.freshmeal.common.BusinessException;
import com.freshmeal.entity.InventoryLog;
import com.freshmeal.entity.OrderItem;
import com.freshmeal.entity.Product;
import com.freshmeal.entity.ProductBatch;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.mapper.InventoryLogMapper;
import com.freshmeal.mapper.ProductBatchMapper;
import com.freshmeal.mapper.ProductMapper;
import com.freshmeal.service.InventoryService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final int CHANGE_OUT = 2;
    private static final int CHANGE_CANCEL_RESTORE = 6;
    private static final String REF_TYPE_ORDER = "ORDER";

    private final ProductMapper productMapper;
    private final ProductBatchMapper productBatchMapper;
    private final InventoryLogMapper inventoryLogMapper;

    @Override
    public void deductForOrder(List<OrderItem> items, String orderNo, Long operatorId) {
        Map<Long, Integer> quantityByProduct = new LinkedHashMap<>();
        for (OrderItem item : items) {
            quantityByProduct.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        for (Map.Entry<Long, Integer> entry : quantityByProduct.entrySet()) {
            Product product = productMapper.selectById(entry.getKey());
            if (product == null || product.getStatus() == null || product.getStatus() != 1) {
                throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE.getCode(),
                        ErrorCode.PRODUCT_UNAVAILABLE.getMessage());
            }
            int quantity = entry.getValue();
            Integer beforeProductStock = product.getStock();
            if (beforeProductStock == null || beforeProductStock < quantity
                    || productMapper.deductStockOptimistically(product.getId(), quantity) != 1) {
                throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT.getCode(),
                        ErrorCode.INVENTORY_INSUFFICIENT.getMessage());
            }
            insertProductLog(product.getId(), quantity, beforeProductStock,
                    beforeProductStock - quantity, CHANGE_OUT, orderNo, operatorId,
                    "下单扣减商品库存");
            deductBatches(product.getId(), quantity, orderNo, operatorId);
        }
    }

    @Override
    public void restoreForOrder(String orderNo, Long operatorId, String remark) {
        List<InventoryLog> sourceLogs = inventoryLogMapper.selectList(
                Wrappers.<InventoryLog>lambdaQuery()
                        .eq(InventoryLog::getRefNo, orderNo)
                        .eq(InventoryLog::getChangeType, CHANGE_OUT));

        sourceLogs.stream()
                .filter(log -> log.getBatchId() == null)
                .forEach(log -> restoreProduct(log, orderNo, operatorId, remark));
        sourceLogs.stream()
                .filter(log -> log.getBatchId() != null)
                .forEach(log -> restoreBatch(log, orderNo, operatorId, remark));
    }

    private void deductBatches(Long productId, int quantity, String orderNo, Long operatorId) {
        int remaining = quantity;
        List<ProductBatch> batches = productBatchMapper.selectAvailableFefo(productId);
        for (ProductBatch batch : batches) {
            if (remaining == 0) {
                break;
            }
            int deducted = Math.min(remaining, batch.getStock());
            if (productBatchMapper.deductStock(batch.getId(), deducted) != 1) {
                continue;
            }
            insertBatchLog(batch, deducted, CHANGE_OUT, orderNo, operatorId,
                    "下单按FEFO扣减批次库存");
            remaining -= deducted;
        }
        if (remaining > 0) {
            throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT.getCode(),
                    "可用批次库存不足，无法履约");
        }
    }

    private void restoreProduct(InventoryLog source, String orderNo, Long operatorId, String remark) {
        Product product = productMapper.selectById(source.getProductId());
        if (product == null) {
            return;
        }
        int before = product.getStock() == null ? 0 : product.getStock();
        if (productMapper.restoreStock(source.getProductId(), source.getChangeNum()) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(),
                    ErrorCode.INTERNAL_ERROR.getMessage());
        }
        insertProductLog(source.getProductId(), source.getChangeNum(), before,
                before + source.getChangeNum(), CHANGE_CANCEL_RESTORE, orderNo,
                operatorId, remark);
    }

    private void restoreBatch(InventoryLog source, String orderNo, Long operatorId, String remark) {
        ProductBatch batch = productBatchMapper.selectById(source.getBatchId());
        if (batch == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(),
                    ErrorCode.INTERNAL_ERROR.getMessage());
        }
        int before = batch.getStock() == null ? 0 : batch.getStock();
        if (productBatchMapper.restoreStock(batch.getId(), source.getChangeNum()) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(),
                    ErrorCode.INTERNAL_ERROR.getMessage());
        }
        InventoryLog log = new InventoryLog();
        log.setProductId(batch.getProductId());
        log.setBatchId(batch.getId());
        log.setChangeType(CHANGE_CANCEL_RESTORE);
        log.setChangeNum(source.getChangeNum());
        log.setBeforeStock(before);
        log.setAfterStock(before + source.getChangeNum());
        log.setRefType(REF_TYPE_ORDER);
        log.setRefNo(orderNo);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        inventoryLogMapper.insert(log);
    }

    private void insertProductLog(Long productId, int quantity, int before, int after,
            int changeType, String orderNo, Long operatorId, String remark) {
        InventoryLog log = new InventoryLog();
        log.setProductId(productId);
        log.setBatchId(null);
        log.setChangeType(changeType);
        log.setChangeNum(quantity);
        log.setBeforeStock(before);
        log.setAfterStock(after);
        log.setRefType(REF_TYPE_ORDER);
        log.setRefNo(orderNo);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        inventoryLogMapper.insert(log);
    }

    private void insertBatchLog(ProductBatch batch, int quantity, int changeType,
            String orderNo, Long operatorId, String remark) {
        ProductBatch updated = productBatchMapper.selectById(batch.getId());
        if (updated == null || Objects.isNull(updated.getStock())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(),
                    ErrorCode.INTERNAL_ERROR.getMessage());
        }
        InventoryLog log = new InventoryLog();
        log.setProductId(batch.getProductId());
        log.setBatchId(batch.getId());
        log.setChangeType(changeType);
        log.setChangeNum(quantity);
        log.setBeforeStock(batch.getStock());
        log.setAfterStock(updated.getStock());
        log.setRefType(REF_TYPE_ORDER);
        log.setRefNo(orderNo);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        inventoryLogMapper.insert(log);
    }
}
```

## A.8 订单服务核心（service/impl/OrderServiceImpl.java）

```java
// 结算预览、提交订单、取消与三个定时任务（省略查询与 VO 组装辅助方法）
    @Override
    public OrderSettleVO settle(Long userId, OrderSettleRequest request) {
        validateDeliveryType(request.deliveryType());
        Settlement settlement = buildSettlement(userId, request.addressId(),
                request.deliveryType(), request.cartItemIds(),
                request.userCouponId());
        return new OrderSettleVO(
                settlement.lines().stream().map(PricedCartLine::line).toList(),
                settlement.address(),
                settlement.freight(),
                settlement.goodsAmount(),
                settlement.freight().freightAmount(),
                settlement.discountAmount(),
                settlement.payAmount(),
                settlement.totalCalories()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO create(Long userId, OrderCreateRequest request) {
        validateDeliveryType(request.deliveryType());
        Settlement settlement = buildSettlement(userId, request.addressId(),
                request.deliveryType(), request.cartItemIds(),
                request.userCouponId());
        DeliverySlot slot = validateSlot(request.deliveryType(),
                request.deliverySlotId(), request.deliverySlotDate());

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setGoodsAmount(settlement.goodsAmount());
        order.setFreightAmount(settlement.freight().freightAmount());
        order.setDiscountAmount(settlement.discountAmount());
        order.setPayAmount(settlement.payAmount());
        order.setDeliveryManFee(BigDecimal.ZERO);
        order.setReceiver(settlement.address().receiver());
        order.setReceiverPhone(settlement.address().phone());
        order.setReceiverAddress(settlement.address().fullAddress());
        order.setLongitude(settlement.address().longitude());
        order.setLatitude(settlement.address().latitude());
        order.setDeliveryType(request.deliveryType());
        order.setExpectTime(slot == null
                ? null
                : request.deliverySlotDate().atTime(slot.getStartTime()));
        order.setDeliverySlotId(request.deliverySlotId());
        order.setDeliverySlotDate(request.deliverySlotDate());
        order.setDeliverySlotName(slot == null ? null : slot.getName());
        order.setDeliveryAreaId(settlement.freight().areaId());
        order.setDeliveryDistance(settlement.freight().distance());
        order.setStatus(OrderStatus.WAIT_PAY.getCode());
        order.setPayStatus(0);
        order.setPayType(0);
        order.setRemark(StringUtils.hasText(request.remark()) ? request.remark().trim() : null);
        order.setUserCouponId(settlement.userCouponId());
        orderMapper.insert(order);
        couponService.markUsed(settlement.userCouponId(), order.getId());

        List<OrderItem> items = settlement.lines().stream()
                .map(line -> toOrderItem(order, line))
                .toList();
        items.forEach(orderItemMapper::insert);
        inventoryService.deductForOrder(items, order.getOrderNo(), userId);

        request.cartItemIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(cartId -> {
                    com.freshmeal.entity.Cart cart = cartMapper.selectById(cartId);
                    if (cart == null || !userId.equals(cart.getUserId())) {
                        throw new BusinessException(ErrorCode.CART_ITEM_UNAVAILABLE.getCode(),
                                ErrorCode.CART_ITEM_UNAVAILABLE.getMessage());
                    }
                    return cart;
                })
                .toList()
                .forEach(cartMapper::deleteById);

        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(order.getId());
        log.setOrderNo(order.getOrderNo());
        log.setFromStatus(-1);
        log.setToStatus(OrderStatus.WAIT_PAY.getCode());
        log.setOperatorType(1);
        log.setOperatorId(userId);
        log.setOperatorName("用户");
        log.setRemark("订单创建并完成服务端计价");
        orderStatusLogMapper.insert(log);
        return detail(userId, order.getOrderNo());
    }

    @Override
    public PageResult<OrderVO> page(Long userId, OrderQuery query) {
        if (query.getStatus() != null
                && (query.getStatus() < 0 || query.getStatus() > 9)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "订单状态筛选值不合法");
        }
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Order> page =
                orderMapper.selectPage(
                        new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                                query.getCurrent(), query.getSize()),
                        Wrappers.<Order>lambdaQuery()
                                .eq(Order::getUserId, userId)
                                .eq(query.getStatus() != null, Order::getStatus,
                                        query.getStatus())
                                .like(StringUtils.hasText(query.getOrderNo()),
                                        Order::getOrderNo, query.getOrderNo().trim())
                                .orderByDesc(Order::getCreateTime)
                                .orderByDesc(Order::getId));
        List<OrderVO> records = page.getRecords().stream()
                .map(order -> toVo(order, false))
                .toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public OrderVO detail(Long userId, String orderNo) {
        Order order = requireOwned(userId, orderNo);
        return toVo(order, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, String orderNo, String reason) {
        Order order = orderMapper.selectByOrderNoForUpdate(orderNo);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND.getCode(),
                    ErrorCode.ORDER_NOT_FOUND.getMessage());
        }
        if (order.getStatus() != OrderStatus.WAIT_PAY.getCode()
                && order.getStatus() != OrderStatus.WAIT_ACCEPT.getCode()) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID.getCode(),
                    ErrorCode.ORDER_STATE_INVALID.getMessage());
        }
        String cancelReason = StringUtils.hasText(reason) ? reason.trim() : "用户取消订单";
        orderStatusManager.transfer(order, OrderStatus.CANCELLED,
                OrderOperator.user(userId, cancelReason));
    }

    @Override
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    @Transactional(rollbackFor = Exception.class)
    public void cancelExpiredUnpaidOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(payExpireMinutes);
        List<Order> timeouts = orderMapper.selectUnpaidTimeouts(cutoff);
        for (Order candidate : timeouts) {
            Order order = orderMapper.selectByOrderNoForUpdate(candidate.getOrderNo());
            if (order == null || order.getStatus() != OrderStatus.WAIT_PAY.getCode()) {
                continue;
            }
            orderStatusManager.transfer(order, OrderStatus.CANCELLED,
                    OrderOperator.system("支付超时自动取消"));
        }
    }

    @Override
    @Scheduled(fixedDelay = 15_000, initialDelay = 15_000)
    @Transactional(rollbackFor = Exception.class)
    public void advanceReadyOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(prepareDelaySeconds);
        orderMapper.selectPaidOrdersReadyForPreparation(cutoff).forEach(candidate -> {
            Order order = orderMapper.selectByOrderNoForUpdate(candidate.getOrderNo());
            if (order == null || order.getStatus() != OrderStatus.WAIT_ACCEPT.getCode()) {
                return;
            }
            orderStatusManager.transfer(order, OrderStatus.PREPARING,
                    OrderOperator.system("商家备餐完成"));
            if (order.getDeliveryType() == DELIVERY_TYPE_EXTERNAL) {
                orderStatusManager.transfer(order, OrderStatus.WAIT_SLOT,
                        OrderOperator.system("进入预约配送时段等待"));
            }
        });
    }

    @Override
    @Scheduled(fixedDelay = 10_000, initialDelay = 20_000)
    @Transactional(rollbackFor = Exception.class)
    public void activateDueSlotOrders() {
        orderMapper.selectDueSlotOrders().forEach(candidate -> {
            Order order = orderMapper.selectByOrderNoForUpdate(candidate.getOrderNo());
            if (order == null || order.getStatus() != OrderStatus.WAIT_SLOT.getCode()) {
                return;
            }
            orderStatusManager.transfer(order, OrderStatus.WAIT_DELIVER,
                    OrderOperator.system("预约时段开始，自动激活"));
        });
    }

    private Settlement buildSettlement(Long userId, Long addressId, int deliveryType,
            List<Long> cartItemIds, Long userCouponId) {
        Address address = addressMapper.selectById(addressId);
        if (address == null || !userId.equals(address.getUserId())) {
            throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND.getCode(),
                    ErrorCode.ADDRESS_NOT_FOUND.getMessage());
        }
        List<PricedCartLine> lines = cartService.priceSelected(userId, cartItemIds, true);
        if (lines.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_ITEM_UNAVAILABLE.getCode(),
                    ErrorCode.CART_ITEM_UNAVAILABLE.getMessage());
        }
        BigDecimal goodsAmount = lines.stream()
                .map(line -> line.line().subtotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        FreightVO freight;
        if (deliveryType == DELIVERY_TYPE_EXTERNAL) {
            freight = freightService.calculate(address, goodsAmount, LocalDateTime.now());
        } else {
            freight = new FreightVO(null, "到店自提", BigDecimal.ZERO,
                    BigDecimal.ZERO, false);
        }
        BigDecimal discount = couponService.calculateDiscount(
                userId, userCouponId, goodsAmount);
        BigDecimal payAmount = goodsAmount
                .add(freight.freightAmount())
                .subtract(discount)
                .setScale(2);
        int calories = lines.stream()
                .mapToInt(line -> line.line().subtotalCalories())
                .sum();
        return new Settlement(lines, toAddressVo(address), freight, goodsAmount,
                discount, payAmount, calories, userCouponId);
    }
```

## A.9 派单与抢单（service/impl/DeliveryOrderServiceImpl.java）

```java
// 抢单、取货、送达上报（省略 VO 组装与校验辅助方法）
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiderDeliveryVO grab(Long riderId, String deliveryNo) {
        DeliveryOrder delivery =
                deliveryOrderMapper.selectByDeliveryNoForUpdate(deliveryNo);
        requireUnassigned(delivery);
        Order order = requireOrder(delivery.getOrderId());
        requireRiderEligible(riderId, false);
        DeliveryMan rider = deliveryManMapper.selectById(riderId);
        requireServiceArea(rider, order);
        persistScoredCandidates(order, delivery,
                scoreCandidates(order, false), riderId, ASSIGN_TYPE_GRAB);
        claim(delivery, rider, ASSIGN_TYPE_GRAB);
        insertTrace(delivery, DeliveryStatus.ASSIGNED,
                "FreshMeal 门店", rider.getRealName() + " 已抢单");
        return toRiderVo(delivery);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiderDeliveryVO pickup(Long riderId, String deliveryNo) {
        DeliveryOrder delivery =
                deliveryOrderMapper.selectByDeliveryNoForUpdate(deliveryNo);
        requireOwnedActiveDelivery(delivery, riderId, DeliveryStatus.ASSIGNED);
        if (deliveryOrderMapper.markPickedUp(delivery.getId(), delivery.getPickupAddress()) != 1) {
            throw conflict("配送单状态已变化，请刷新后重试");
        }
        if (deliveryOrderMapper.transitionStatus(delivery.getId(),
                DeliveryStatus.ASSIGNED.getCode(),
                DeliveryStatus.PICKING_UP.getCode()) != 1) {
            throw conflict("配送单状态已变化，请刷新后重试");
        }
        delivery.setStatus(DeliveryStatus.PICKING_UP.getCode());
        delivery.setPickupTime(LocalDateTime.now());
        insertTrace(delivery, DeliveryStatus.PICKING_UP,
                delivery.getPickupAddress(), "配送员已完成取货");
        Order order = requireOrder(delivery.getOrderId());
        orderStatusManager.transfer(order, OrderStatus.DELIVERING,
                OrderOperator.rider(riderId, "配送员确认取货"));
        return toRiderVo(reload(delivery.getDeliveryNo()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiderDeliveryVO deliver(Long riderId, String deliveryNo) {
        DeliveryOrder delivery =
                deliveryOrderMapper.selectByDeliveryNoForUpdate(deliveryNo);
        requireOwnedActiveDelivery(delivery, riderId, DeliveryStatus.DELIVERING);
        if (deliveryOrderMapper.markReceived(delivery.getId()) != 1) {
            throw conflict("配送单状态已变化，请刷新后重试");
        }
        if (deliveryOrderMapper.transitionStatus(delivery.getId(),
                DeliveryStatus.DELIVERING.getCode(),
                DeliveryStatus.DELIVERED.getCode()) != 1) {
            throw conflict("配送单状态已变化，请刷新后重试");
        }
        delivery.setStatus(DeliveryStatus.DELIVERED.getCode());
        delivery.setReceiveTime(LocalDateTime.now());
        insertTrace(delivery, DeliveryStatus.DELIVERED,
                requireOrder(delivery.getOrderId()).getReceiverAddress(),
                "配送员确认送达");
        Order order = requireOrder(delivery.getOrderId());
        orderStatusManager.transfer(order, OrderStatus.FINISHED,
                OrderOperator.rider(riderId, "配送员确认送达"));
        return toRiderVo(reload(delivery.getDeliveryNo()));
    }

    @Override

// 定时派单与多因子评分（省略候选日志持久化辅助方法）
    @Override
    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    @Transactional(rollbackFor = Exception.class)
    public void dispatchWaitingOrders() {
        deliveryOrderMapper.selectWaiting(100).forEach(delivery -> {
            DeliveryOrder locked =
                    deliveryOrderMapper.selectByOrderIdForUpdate(delivery.getOrderId());
            if (locked == null || locked.getStatus() != DeliveryStatus.WAIT_ASSIGN.getCode()) {
                return;
            }
            Order order = orderMapper.selectById(locked.getOrderId());
            if (order != null && order.getStatus() == OrderStatus.WAIT_DELIVER.getCode()) {
                dispatch(locked, order, ASSIGN_TYPE_SYSTEM, true);
            }
        });
    }

    private void dispatch(DeliveryOrder delivery, Order order, int assignType,
            boolean persistCandidates) {
        List<ScoredCandidate> candidates = scoreCandidates(order, false);
        if (candidates.isEmpty()) {
            return;
        }
        ScoredCandidate selected = candidates.get(0);
        if (deliveryOrderMapper.claimForRider(
                delivery.getId(), selected.rider().getId(),
                selected.rider().getRealName(), selected.rider().getPhone(),
                assignType) != 1) {
            return;
        }
        fillClaimed(delivery, selected.rider(), assignType);
        updateRiderBusy(selected.rider());
        if (persistCandidates) {
            persistCandidates(delivery, candidates, selected.rider().getId());
        }
        insertTrace(delivery, DeliveryStatus.ASSIGNED,
                "FreshMeal 门店", selected.rider().getRealName() + " 已接单");
    }

    private List<ScoredCandidate> scoreCandidates(Order order, boolean allowBusy) {
        return deliveryManMapper.selectList(Wrappers.<DeliveryMan>lambdaQuery()
                        .eq(DeliveryMan::getStatus, 1)
                        .eq(DeliveryMan::getOnlineStatus, 1)
                        .in(DeliveryMan::getWorkStatus, allowBusy
                                ? List.of(0, 1) : List.of(0)))
                .stream()
                .filter(rider -> rider.getServiceAreaId() == null
                        || rider.getServiceAreaId().equals(order.getDeliveryAreaId()))
                .filter(rider -> rider.getMaxOrderNum() != null
                        && rider.getMaxOrderNum() > 0
                        && inTransitCount(rider.getId()) < rider.getMaxOrderNum())
                .map(rider -> score(order, rider))
                .sorted(Comparator.comparingDouble(ScoredCandidate::totalScore).reversed())
                .toList();
    }

    private ScoredCandidate score(Order order, DeliveryMan rider) {
        double distance = distanceKm(rider, order);
        double distanceScore = Math.max(0, 1 - distance / MAX_DISTANCE_KM);
        int load = inTransitCount(rider.getId());
        double loadScore = 1 - (double) load / rider.getMaxOrderNum();
        BigDecimal rating = rider.getRating() == null
                ? BigDecimal.valueOf(5) : rider.getRating();
        double ratingScore = rating.doubleValue() / 5.0;
        double total = 0.5 * distanceScore + 0.3 * loadScore + 0.2 * ratingScore;
        return new ScoredCandidate(rider, total, distanceScore,
                loadScore, ratingScore, distance, load);
    }

    private void claim(DeliveryOrder delivery, DeliveryMan rider, int assignType) {
        if (deliveryOrderMapper.claimForRider(delivery.getId(), rider.getId(),
                rider.getRealName(), rider.getPhone(), assignType) != 1) {
            throw conflict("该配送单已被其他配送员承接");
        }
        fillClaimed(delivery, rider, assignType);
        updateRiderBusy(rider);
    }

    private void fillClaimed(DeliveryOrder delivery, DeliveryMan rider, int assignType) {
        delivery.setDeliveryManId(rider.getId());
        delivery.setDeliveryManName(rider.getRealName());
        delivery.setDeliveryManPhone(rider.getPhone());
        delivery.setStatus(DeliveryStatus.ASSIGNED.getCode());
        delivery.setAssignType(assignType);
        delivery.setAssignTime(LocalDateTime.now());
    }
```
