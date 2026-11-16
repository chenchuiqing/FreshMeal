package com.freshmeal.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.freshmeal.common.BusinessException;
import com.freshmeal.common.PageResult;
import com.freshmeal.dto.user.OrderCreateRequest;
import com.freshmeal.dto.user.OrderQuery;
import com.freshmeal.dto.user.OrderSettleRequest;
import com.freshmeal.entity.Address;
import com.freshmeal.entity.DeliverySlot;
import com.freshmeal.entity.Order;
import com.freshmeal.entity.OrderItem;
import com.freshmeal.entity.OrderStatusLog;
import com.freshmeal.entity.Payment;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.enums.OrderStatus;
import com.freshmeal.mapper.AddressMapper;
import com.freshmeal.mapper.CartMapper;
import com.freshmeal.mapper.OrderItemMapper;
import com.freshmeal.mapper.OrderMapper;
import com.freshmeal.mapper.OrderStatusLogMapper;
import com.freshmeal.mapper.PaymentMapper;
import com.freshmeal.service.CartService;
import com.freshmeal.service.CouponService;
import com.freshmeal.service.DeliverySlotService;
import com.freshmeal.service.FreightService;
import com.freshmeal.service.InventoryService;
import com.freshmeal.service.OrderOperator;
import com.freshmeal.service.OrderService;
import com.freshmeal.service.OrderStatusManager;
import com.freshmeal.service.PricedCartLine;
import com.freshmeal.vo.user.AddressVO;
import com.freshmeal.vo.user.CartLineVO;
import com.freshmeal.vo.user.FreightVO;
import com.freshmeal.vo.user.OrderItemVO;
import com.freshmeal.vo.user.OrderSettleVO;
import com.freshmeal.vo.user.OrderTimelineVO;
import com.freshmeal.vo.user.OrderVO;
import com.freshmeal.vo.user.PaymentVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final int DELIVERY_TYPE_EXTERNAL = 1;
    private static final int DELIVERY_TYPE_PICKUP = 2;
    private static final DateTimeFormatter ORDER_NO_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final PaymentMapper paymentMapper;
    private final AddressMapper addressMapper;
    private final CartMapper cartMapper;
    private final CartService cartService;
    private final CouponService couponService;
    private final DeliverySlotService deliverySlotService;
    private final FreightService freightService;
    private final InventoryService inventoryService;
    private final OrderStatusManager orderStatusManager;
    private final ObjectMapper objectMapper;

    @Value("${freshmeal.order.pay-expire-minutes:15}")
    private int payExpireMinutes;

    @Value("${freshmeal.order.prepare-delay-seconds:10}")
    private long prepareDelaySeconds;

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
        String orderNoFilter = StringUtils.hasText(query.getOrderNo())
                ? query.getOrderNo().trim() : null;
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Order> page =
                orderMapper.selectPage(
                        new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                                query.getCurrent(), query.getSize()),
                        Wrappers.<Order>lambdaQuery()
                                .eq(Order::getUserId, userId)
                                .eq(query.getStatus() != null, Order::getStatus,
                                        query.getStatus())
                                .like(orderNoFilter != null,
                                        Order::getOrderNo, orderNoFilter)
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

    private OrderItem toOrderItem(Order order, PricedCartLine priced) {
        CartLineVO line = priced.line();
        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setOrderNo(order.getOrderNo());
        item.setUserId(order.getUserId());
        item.setProductId(line.productId());
        item.setProductName(line.productName());
        item.setProductCover(line.cover());
        item.setSpecId(line.specId());
        item.setSpecName(line.specName());
        item.setOptions(writeJson(line.options()));
        item.setOptionsDesc(line.optionsDesc());
        item.setPrice(line.unitPrice());
        item.setQuantity(line.quantity());
        item.setSubtotal(line.subtotal());
        item.setCalories(line.calories());
        return item;
    }

    private OrderVO toVo(Order order, boolean includeTimeline) {
        List<OrderItem> items = orderItemMapper.selectList(
                Wrappers.<OrderItem>lambdaQuery()
                        .eq(OrderItem::getOrderId, order.getId())
                        .orderByAsc(OrderItem::getId));
        List<OrderTimelineVO> timeline = includeTimeline ? timeline(order.getId()) : List.of();
        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getOrderId, order.getId())
                .orderByDesc(Payment::getId)
                .last("LIMIT 1"));
        return new OrderVO(
                order.getId(),
                order.getOrderNo(),
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
                order.getRemark(),
                order.getCancelReason(),
                order.getCreateTime(),
                order.getPayTime(),
                order.getCancelTime(),
                payment == null ? null : toPaymentVo(payment),
                items.stream().map(this::toItemVo).toList(),
                timeline
        );
    }

    private List<OrderTimelineVO> timeline(Long orderId) {
        return orderStatusLogMapper.selectList(Wrappers.<OrderStatusLog>lambdaQuery()
                        .eq(OrderStatusLog::getOrderId, orderId)
                        .orderByAsc(OrderStatusLog::getCreateTime)
                        .orderByAsc(OrderStatusLog::getId))
                .stream()
                .map(log -> new OrderTimelineVO(
                        log.getToStatus(),
                        log.getFromStatus() == -1
                                ? "订单创建"
                                : OrderStatus.of(log.getToStatus()).getDescription(),
                        log.getOperatorName(),
                        log.getRemark(),
                        log.getCreateTime()
                ))
                .toList();
    }

    private OrderItemVO toItemVo(OrderItem item) {
        return new OrderItemVO(
                item.getProductId(),
                item.getProductName(),
                item.getProductCover(),
                item.getSpecName(),
                item.getOptionsDesc(),
                item.getPrice(),
                item.getQuantity(),
                item.getSubtotal(),
                item.getCalories(),
                item.getCalories() * item.getQuantity()
        );
    }

    private PaymentVO toPaymentVo(Payment payment) {
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

    private AddressVO toAddressVo(Address address) {
        return new AddressVO(
                address.getId(),
                address.getReceiver(),
                address.getPhone(),
                address.getProvince(),
                address.getCity(),
                address.getDistrict(),
                address.getDetail(),
                address.getProvince() + address.getCity() + address.getDistrict()
                        + address.getDetail(),
                address.getLongitude(),
                address.getLatitude(),
                address.getTag(),
                address.getIsDefault() != null && address.getIsDefault() == 1
        );
    }

    private Order requireOwned(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderNo, orderNo));
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND.getCode(),
                    ErrorCode.ORDER_NOT_FOUND.getMessage());
        }
        return order;
    }

    private void validateDeliveryType(Integer deliveryType) {
        if (deliveryType == null
                || deliveryType != DELIVERY_TYPE_EXTERNAL
                && deliveryType != DELIVERY_TYPE_PICKUP) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "配送方式不合法");
        }
    }

    private DeliverySlot validateSlot(int deliveryType, Long slotId, LocalDate slotDate) {
        if (deliveryType == DELIVERY_TYPE_PICKUP) {
            if (slotId != null || slotDate != null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                        "到店自提不能选择配送时段");
            }
            return null;
        }
        if (slotId == null || slotDate == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "请选择配送日期与时段");
        }
        if (slotId == null && slotDate != null || slotId != null && slotDate == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "预约配送时段不完整");
        }
        return deliverySlotService.validateAndSnapshot(slotId, slotDate);
    }

    private String generateOrderNo() {
        String time = LocalDateTime.now().format(ORDER_NO_TIME);
        String random = String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        return "FM" + time + random;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(),
                    ErrorCode.INTERNAL_ERROR.getMessage());
        }
    }

    private record Settlement(
            List<PricedCartLine> lines,
            AddressVO address,
            FreightVO freight,
            BigDecimal goodsAmount,
            BigDecimal discountAmount,
            BigDecimal payAmount,
        int totalCalories,
        Long userCouponId
    ) {
    }
}
