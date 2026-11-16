package com.freshmeal.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freshmeal.common.BusinessException;
import com.freshmeal.common.PageResult;
import com.freshmeal.dto.admin.AdminDeliveryQuery;
import com.freshmeal.entity.DeliveryMan;
import com.freshmeal.entity.DeliveryManLog;
import com.freshmeal.entity.DeliveryOrder;
import com.freshmeal.entity.DeliverySlot;
import com.freshmeal.entity.DeliveryTrace;
import com.freshmeal.entity.Order;
import com.freshmeal.enums.DeliveryStatus;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.enums.OrderStatus;
import com.freshmeal.mapper.DeliveryManLogMapper;
import com.freshmeal.mapper.DeliveryManMapper;
import com.freshmeal.mapper.DeliveryOrderMapper;
import com.freshmeal.mapper.DeliverySlotMapper;
import com.freshmeal.mapper.DeliveryTraceMapper;
import com.freshmeal.mapper.OrderMapper;
import com.freshmeal.service.DeliveryOrderService;
import com.freshmeal.service.OrderOperator;
import com.freshmeal.service.OrderStatusManager;
import com.freshmeal.vo.admin.AdminDeliveryVO;
import com.freshmeal.vo.admin.AdminRiderVO;
import com.freshmeal.vo.rider.RiderDeliveryVO;
import com.freshmeal.vo.rider.RiderStatisticsVO;
import com.freshmeal.vo.rider.RiderWorkStatusVO;
import com.freshmeal.vo.user.DeliveryTraceVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeliveryOrderServiceImpl implements DeliveryOrderService {

    private static final int ASSIGN_TYPE_SYSTEM = 1;
    private static final int ASSIGN_TYPE_GRAB = 2;
    private static final int ASSIGN_TYPE_MANUAL = 3;
    private static final double MAX_DISTANCE_KM = 10.0;
    private static final DateTimeFormatter DELIVERY_NO_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT);

    private final DeliveryOrderMapper deliveryOrderMapper;
    private final DeliveryTraceMapper deliveryTraceMapper;
    private final DeliveryManLogMapper deliveryManLogMapper;
    private final DeliveryManMapper deliveryManMapper;
    private final DeliverySlotMapper deliverySlotMapper;
    private final OrderMapper orderMapper;
    private final OrderStatusManager orderStatusManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createForOrder(Order order) {
        if (order.getDeliveryType() == null || order.getDeliveryType() != 1) {
            return;
        }
        DeliveryOrder existing = deliveryOrderMapper.selectOne(
                Wrappers.<DeliveryOrder>lambdaQuery()
                        .eq(DeliveryOrder::getOrderId, order.getId()));
        if (existing != null) {
            return;
        }
        DeliveryOrder delivery = new DeliveryOrder();
        delivery.setDeliveryNo(generateDeliveryNo());
        delivery.setOrderId(order.getId());
        delivery.setOrderNo(order.getOrderNo());
        delivery.setAreaId(order.getDeliveryAreaId());
        delivery.setDistance(zeroIfNull(order.getDeliveryDistance()));
        delivery.setFreight(zeroIfNull(order.getFreightAmount()));
        delivery.setManFee(zeroIfNull(order.getDeliveryManFee()));
        delivery.setStatus(DeliveryStatus.WAIT_ASSIGN.getCode());
        delivery.setAssignType(0);
        delivery.setPickupAddress("FreshMeal 门店");
        deliveryOrderMapper.insert(delivery);
        insertTrace(delivery, DeliveryStatus.WAIT_ASSIGN,
                "FreshMeal 门店", "配送单已生成，等待分配配送员");
        dispatch(delivery, order, ASSIGN_TYPE_SYSTEM, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncForOrder(Order order, DeliveryStatus targetStatus) {
        DeliveryOrder delivery = deliveryOrderMapper.selectByOrderIdForUpdate(order.getId());
        if (delivery == null) {
            return;
        }
        if (targetStatus == DeliveryStatus.DELIVERING
                && delivery.getStatus() == DeliveryStatus.PICKING_UP.getCode()) {
            transition(delivery, DeliveryStatus.PICKING_UP, DeliveryStatus.DELIVERING);
            insertTrace(delivery, DeliveryStatus.DELIVERING,
                    "配送途中", "配送员已取货，正在前往收货地址");
        } else if (targetStatus == DeliveryStatus.DELIVERED
                && delivery.getStatus() != DeliveryStatus.DELIVERED.getCode()) {
            deliveryOrderMapper.markReceived(delivery.getId());
            delivery.setStatus(DeliveryStatus.DELIVERED.getCode());
            delivery.setReceiveTime(LocalDateTime.now());
            insertTrace(delivery, DeliveryStatus.DELIVERED,
                    order.getReceiverAddress(), "订单已送达");
        }
        if (targetStatus == DeliveryStatus.DELIVERED) {
            completeRiderStatistics(delivery.getDeliveryManId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelForOrder(Order order) {
        DeliveryOrder delivery = deliveryOrderMapper.selectOne(
                Wrappers.<DeliveryOrder>lambdaQuery()
                        .eq(DeliveryOrder::getOrderId, order.getId()));
        if (delivery == null) {
            return;
        }
        if (delivery.getStatus() != DeliveryStatus.WAIT_ASSIGN.getCode()
                && delivery.getStatus() != DeliveryStatus.ASSIGNED.getCode()) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID.getCode(),
                    "配送任务已进入取送流程，不能随订单取消");
        }
        Long riderId = delivery.getDeliveryManId();
        deliveryTraceMapper.delete(Wrappers.<DeliveryTrace>lambdaQuery()
                .eq(DeliveryTrace::getDeliveryOrderId, delivery.getId()));
        deliveryOrderMapper.deleteById(delivery.getId());
        if (riderId != null && !hasActiveTasks(riderId)) {
            DeliveryMan rider = deliveryManMapper.selectById(riderId);
            if (rider != null) {
                rider.setWorkStatus(0);
                deliveryManMapper.updateById(rider);
            }
        }
    }

    @Override
    public List<RiderDeliveryVO> todo(Long riderId) {
        DeliveryMan rider = deliveryManMapper.selectById(riderId);
        if (rider == null || rider.getStatus() == null || rider.getStatus() != 1
                || rider.getOnlineStatus() == null || rider.getOnlineStatus() != 1
                || rider.getWorkStatus() == null || rider.getWorkStatus() != 0) {
            return List.of();
        }
        return deliveryOrderMapper.selectWaiting(100).stream()
                .filter(delivery -> {
                    Order order = orderMapper.selectById(delivery.getOrderId());
                    return order != null
                            && (rider.getServiceAreaId() == null
                            || rider.getServiceAreaId().equals(order.getDeliveryAreaId()));
                })
                .filter(delivery -> rider.getMaxOrderNum() != null
                        && inTransitCount(riderId) < rider.getMaxOrderNum())
                .map(this::toRiderVo)
                .toList();
    }

    @Override
    public List<RiderDeliveryVO> myTasks(Long riderId) {
        return deliveryOrderMapper.selectList(Wrappers.<DeliveryOrder>lambdaQuery()
                        .eq(DeliveryOrder::getDeliveryManId, riderId)
                        .between(DeliveryOrder::getStatus,
                                DeliveryStatus.ASSIGNED.getCode(),
                                DeliveryStatus.DELIVERED.getCode())
                        .orderByDesc(DeliveryOrder::getUpdateTime)
                        .orderByDesc(DeliveryOrder::getId)
                        .last("LIMIT 100"))
                .stream()
                .map(this::toRiderVo)
                .toList();
    }

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
    public RiderStatisticsVO statistics(Long riderId) {
        DeliveryMan rider = requireRider(riderId);
        long active = deliveryOrderMapper.selectCount(
                Wrappers.<DeliveryOrder>lambdaQuery()
                        .eq(DeliveryOrder::getDeliveryManId, riderId)
                        .between(DeliveryOrder::getStatus,
                                DeliveryStatus.ASSIGNED.getCode(),
                                DeliveryStatus.DELIVERING.getCode()));
        return new RiderStatisticsVO(
                valueOrZero(rider.getTodayOrderCount()),
                valueOrZero(rider.getTotalOrderCount()),
                (int) active,
                zeroIfNull(rider.getTotalIncome()),
                rider.getRating(),
                rider.getOnlineStatus(),
                rider.getWorkStatus()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiderWorkStatusVO updateWorkStatus(
            Long riderId, Integer onlineStatus, Integer workStatus) {
        DeliveryMan rider = requireRider(riderId);
        if (onlineStatus == 0 && workStatus != 2) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "离线时工作状态必须为休息");
        }
        if (onlineStatus == 1 && workStatus == 2
                && hasActiveTasks(riderId)) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(),
                    "仍有配送任务时不能休息");
        }
        rider.setOnlineStatus(onlineStatus);
        rider.setWorkStatus(workStatus);
        deliveryManMapper.updateById(rider);
        return new RiderWorkStatusVO(onlineStatus, workStatus);
    }

    @Override
    public PageResult<AdminDeliveryVO> adminPage(AdminDeliveryQuery query) {
        String orderNoFilter = query.getOrderNo() != null && !query.getOrderNo().isBlank()
                ? query.getOrderNo().trim() : null;
        Page<DeliveryOrder> page = deliveryOrderMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                Wrappers.<DeliveryOrder>lambdaQuery()
                        .eq(query.getStatus() != null,
                                DeliveryOrder::getStatus, query.getStatus())
                        .like(orderNoFilter != null,
                                DeliveryOrder::getOrderNo, orderNoFilter)
                        .orderByDesc(DeliveryOrder::getCreateTime)
                        .orderByDesc(DeliveryOrder::getId));
        List<AdminDeliveryVO> records = page.getRecords().stream()
                .map(this::toAdminVo)
                .toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminDeliveryVO adminAssign(Long deliveryId, Long riderId) {
        DeliveryOrder delivery = deliveryOrderMapper.selectById(deliveryId);
        if (delivery == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(),
                    "配送单不存在");
        }
        delivery = deliveryOrderMapper.selectByOrderIdForUpdate(delivery.getOrderId());
        requireUnassigned(delivery);
        Order order = requireOrder(delivery.getOrderId());
        requireRiderEligible(riderId, true);
        DeliveryMan rider = deliveryManMapper.selectById(riderId);
        requireServiceArea(rider, order);
        persistScoredCandidates(order, delivery,
                scoreCandidates(order, true), riderId, ASSIGN_TYPE_MANUAL);
        claim(delivery, rider, ASSIGN_TYPE_MANUAL);
        insertTrace(delivery, DeliveryStatus.ASSIGNED,
                "FreshMeal 门店", "管理员已指派 " + rider.getRealName());
        return toAdminVo(delivery);
    }

    @Override
    public List<AdminRiderVO> adminRiders() {
        return deliveryManMapper.selectList(Wrappers.<DeliveryMan>lambdaQuery()
                        .eq(DeliveryMan::getStatus, 1)
                        .orderByAsc(DeliveryMan::getRealName))
                .stream()
                .map(rider -> {
                    int inTransit = inTransitCount(rider.getId());
                    boolean assignable = rider.getOnlineStatus() != null
                            && rider.getOnlineStatus() == 1
                            && rider.getWorkStatus() != null
                            && (rider.getWorkStatus() == 0 || rider.getWorkStatus() == 1)
                            && rider.getMaxOrderNum() != null
                            && inTransit < rider.getMaxOrderNum();
                    return new AdminRiderVO(
                            rider.getId(),
                            rider.getRealName(),
                            rider.getPhone(),
                            rider.getOnlineStatus(),
                            rider.getWorkStatus(),
                            rider.getMaxOrderNum(),
                            inTransit,
                            rider.getServiceAreaId(),
                            assignable
                    );
                })
                .toList();
    }

    @Override
    public List<DeliveryTraceVO> traceForUser(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderNo, orderNo));
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND.getCode(),
                    ErrorCode.ORDER_NOT_FOUND.getMessage());
        }
        DeliveryOrder delivery = deliveryOrderMapper.selectOne(
                Wrappers.<DeliveryOrder>lambdaQuery()
                        .eq(DeliveryOrder::getOrderId, order.getId()));
        if (delivery == null) {
            return List.of();
        }
        return deliveryTraceMapper.selectList(Wrappers.<DeliveryTrace>lambdaQuery()
                        .eq(DeliveryTrace::getDeliveryOrderId, delivery.getId())
                        .orderByAsc(DeliveryTrace::getCreateTime)
                        .orderByAsc(DeliveryTrace::getId))
                .stream()
                .map(trace -> new DeliveryTraceVO(
                        trace.getStatus(),
                        DeliveryStatus.of(trace.getStatus()).getDescription(),
                        trace.getAddress(),
                        trace.getRemark(),
                        trace.getLongitude(),
                        trace.getLatitude(),
                        trace.getCreateTime()
                ))
                .toList();
    }

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

    private void updateRiderBusy(DeliveryMan rider) {
        rider.setWorkStatus(1);
        deliveryManMapper.updateById(rider);
    }

    private void completeRiderStatistics(Long riderId) {
        if (riderId == null) {
            return;
        }
        DeliveryMan rider = deliveryManMapper.selectById(riderId);
        if (rider == null) {
            return;
        }
        rider.setTodayOrderCount(valueOrZero(rider.getTodayOrderCount()) + 1);
        rider.setTotalOrderCount(valueOrZero(rider.getTotalOrderCount()) + 1);
        rider.setTotalIncome(zeroIfNull(rider.getTotalIncome()));
        if (!hasActiveTasks(riderId)) {
            rider.setWorkStatus(0);
        }
        deliveryManMapper.updateById(rider);
    }

    private void requireUnassigned(DeliveryOrder delivery) {
        if (delivery == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(),
                    "配送单不存在");
        }
        if (delivery.getStatus() != DeliveryStatus.WAIT_ASSIGN.getCode()) {
            throw conflict("该配送单已被承接");
        }
    }

    private void requireOwnedActiveDelivery(
            DeliveryOrder delivery, Long riderId, DeliveryStatus expected) {
        if (delivery == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(),
                    "配送单不存在");
        }
        if (!riderId.equals(delivery.getDeliveryManId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(),
                    "只能操作自己的配送任务");
        }
        if (delivery.getStatus() != expected.getCode()) {
            throw conflict("配送单当前状态不允许该操作");
        }
        Order order = requireOrder(delivery.getOrderId());
        if (expected == DeliveryStatus.ASSIGNED
                && order.getStatus() != OrderStatus.WAIT_DELIVER.getCode()
                || expected == DeliveryStatus.DELIVERING
                && order.getStatus() != OrderStatus.DELIVERING.getCode()) {
            throw conflict("关联订单状态已变化，请刷新后重试");
        }
    }

    private void requireRiderEligible(Long riderId, boolean allowBusy) {
        DeliveryMan rider = requireRider(riderId);
        if (rider.getOnlineStatus() == null || rider.getOnlineStatus() != 1
                || rider.getWorkStatus() == null
                || rider.getWorkStatus() != 0 && (!allowBusy || rider.getWorkStatus() != 1)) {
            throw conflict("该配送员当前不满足接单条件");
        }
        if (rider.getMaxOrderNum() == null || rider.getMaxOrderNum() <= 0
                || inTransitCount(riderId) >= rider.getMaxOrderNum()) {
            throw conflict("该配送员已达到负载上限");
        }
    }

    private void requireServiceArea(DeliveryMan rider, Order order) {
        if (rider.getServiceAreaId() != null
                && !rider.getServiceAreaId().equals(order.getDeliveryAreaId())) {
            throw conflict("该配送员不覆盖订单配送区域");
        }
    }

    private DeliveryMan requireRider(Long riderId) {
        DeliveryMan rider = deliveryManMapper.selectById(riderId);
        if (rider == null || rider.getStatus() == null || rider.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(),
                    "配送员不存在或已禁用");
        }
        return rider;
    }

    private int inTransitCount(Long riderId) {
        return deliveryOrderMapper.selectCount(
                Wrappers.<DeliveryOrder>lambdaQuery()
                        .eq(DeliveryOrder::getDeliveryManId, riderId)
                        .between(DeliveryOrder::getStatus,
                                DeliveryStatus.ASSIGNED.getCode(),
                                DeliveryStatus.DELIVERING.getCode())).intValue();
    }

    private boolean hasActiveTasks(Long riderId) {
        return inTransitCount(riderId) > 0;
    }

    private void persistScoredCandidates(Order order, DeliveryOrder delivery,
            List<ScoredCandidate> candidates, Long selectedId, int assignType) {
        if (candidates.isEmpty()) {
            DeliveryMan selected = deliveryManMapper.selectById(selectedId);
            if (selected != null) {
                candidates = List.of(score(order, selected));
            }
        }
        persistCandidates(delivery, candidates, selectedId);
    }

    private void persistCandidates(DeliveryOrder delivery,
            List<ScoredCandidate> candidates, Long selectedId) {
        for (ScoredCandidate candidate : candidates) {
            DeliveryManLog log = new DeliveryManLog();
            log.setDeliveryOrderId(delivery.getId());
            log.setCandidateId(candidate.rider().getId());
            log.setCandidateName(candidate.rider().getRealName());
            log.setTotalScore(decimal(candidate.totalScore()));
            log.setDistanceScore(decimal(candidate.distanceScore()));
            log.setLoadScore(decimal(candidate.loadScore()));
            log.setRatingScore(decimal(candidate.ratingScore()));
            log.setDistanceValue(decimal(candidate.distance())
                    .setScale(2, RoundingMode.HALF_UP));
            log.setLoadValue(candidate.load());
            log.setIsFinal(candidate.rider().getId().equals(selectedId) ? 1 : 0);
            deliveryManLogMapper.insert(log);
        }
    }

    private void transition(DeliveryOrder delivery, DeliveryStatus from,
            DeliveryStatus to) {
        if (deliveryOrderMapper.transitionStatus(delivery.getId(),
                from.getCode(), to.getCode()) != 1) {
            throw conflict("配送单状态已变化，请刷新后重试");
        }
        delivery.setStatus(to.getCode());
    }

    private void insertTrace(DeliveryOrder delivery, DeliveryStatus status,
            String address, String remark) {
        DeliveryTrace trace = new DeliveryTrace();
        trace.setDeliveryOrderId(delivery.getId());
        trace.setStatus(status.getCode());
        trace.setAddress(address);
        trace.setRemark(remark);
        deliveryTraceMapper.insert(trace);
    }

    private RiderDeliveryVO toRiderVo(DeliveryOrder delivery) {
        Order order = requireOrder(delivery.getOrderId());
        DeliverySlot slot = order.getDeliverySlotId() == null
                ? null : deliverySlotMapper.selectById(order.getDeliverySlotId());
        LocalDateTime slotStart = slot == null || order.getDeliverySlotDate() == null
                ? null
                : order.getDeliverySlotDate().atTime(slot.getStartTime());
        return new RiderDeliveryVO(
                delivery.getId(),
                delivery.getDeliveryNo(),
                delivery.getOrderNo(),
                delivery.getStatus(),
                DeliveryStatus.of(delivery.getStatus()).getDescription(),
                delivery.getAssignType(),
                order.getReceiver(),
                order.getReceiverPhone(),
                order.getReceiverAddress(),
                delivery.getDistance(),
                delivery.getFreight(),
                delivery.getManFee(),
                slotStart,
                delivery.getCreateTime(),
                delivery.getAssignTime(),
                delivery.getPickupTime(),
                delivery.getReceiveTime()
        );
    }

    private AdminDeliveryVO toAdminVo(DeliveryOrder delivery) {
        return new AdminDeliveryVO(
                delivery.getId(),
                delivery.getDeliveryNo(),
                delivery.getOrderNo(),
                delivery.getStatus(),
                DeliveryStatus.of(delivery.getStatus()).getDescription(),
                delivery.getAssignType(),
                assignTypeText(delivery.getAssignType()),
                delivery.getDeliveryManId(),
                delivery.getDeliveryManName(),
                delivery.getDeliveryManPhone(),
                delivery.getDistance(),
                delivery.getFreight(),
                delivery.getManFee(),
                delivery.getAssignTime(),
                delivery.getPickupTime(),
                delivery.getReceiveTime(),
                delivery.getCreateTime()
        );
    }

    private String assignTypeText(Integer assignType) {
        if (assignType == null) {
            return "待分配";
        }
        return switch (assignType) {
            case ASSIGN_TYPE_SYSTEM -> "系统派单";
            case ASSIGN_TYPE_GRAB -> "配送员抢单";
            case ASSIGN_TYPE_MANUAL -> "人工指派";
            default -> "待分配";
        };
    }

    private Order requireOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND.getCode(),
                    ErrorCode.ORDER_NOT_FOUND.getMessage());
        }
        return order;
    }

    private DeliveryOrder reload(String deliveryNo) {
        DeliveryOrder delivery = deliveryOrderMapper.selectOne(
                Wrappers.<DeliveryOrder>lambdaQuery()
                        .eq(DeliveryOrder::getDeliveryNo, deliveryNo));
        if (delivery == null) {
            throw conflict("配送单不存在");
        }
        return delivery;
    }

    private double distanceKm(DeliveryMan rider, Order order) {
        if (rider.getLongitude() == null || rider.getLatitude() == null
                || order.getLongitude() == null || order.getLatitude() == null) {
            return MAX_DISTANCE_KM;
        }
        double lat1 = Math.toRadians(rider.getLatitude().doubleValue());
        double lon1 = Math.toRadians(rider.getLongitude().doubleValue());
        double lat2 = Math.toRadians(order.getLatitude().doubleValue());
        double lon2 = Math.toRadians(order.getLongitude().doubleValue());
        double deltaLat = lat2 - lat1;
        double deltaLon = lon2 - lon1;
        double value = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
    }

    private String generateDeliveryNo() {
        String time = LocalDateTime.now().format(DELIVERY_NO_TIME);
        int random = ThreadLocalRandom.current().nextInt(100_000_000);
        return String.format("DL%s%08d", time, random);
    }

    private BusinessException conflict(String message) {
        return new BusinessException(ErrorCode.CONFLICT.getCode(), message);
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private record ScoredCandidate(
            DeliveryMan rider,
            double totalScore,
            double distanceScore,
            double loadScore,
            double ratingScore,
            double distance,
            int load
    ) {
    }
}
