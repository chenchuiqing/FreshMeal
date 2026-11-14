package com.freshmeal.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.freshmeal.common.BusinessException;
import com.freshmeal.entity.DeliverySlot;
import com.freshmeal.entity.Order;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.enums.OrderStatus;
import com.freshmeal.mapper.DeliverySlotMapper;
import com.freshmeal.mapper.OrderMapper;
import com.freshmeal.service.DeliverySlotService;
import com.freshmeal.vo.user.DeliverySlotVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeliverySlotServiceImpl implements DeliverySlotService {

    private static final int STATUS_ACTIVE_FROM = 0;
    private static final int STATUS_ACTIVE_TO = 7;

    private final DeliverySlotMapper deliverySlotMapper;
    private final OrderMapper orderMapper;

    @Override
    public List<DeliverySlotVO> listAvailable() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        List<DeliverySlot> slots = deliverySlotMapper.selectList(
                Wrappers.<DeliverySlot>lambdaQuery()
                        .eq(DeliverySlot::getStatus, 1)
                        .orderByAsc(DeliverySlot::getSort)
                        .orderByAsc(DeliverySlot::getStartTime));
        if (slots.isEmpty()) {
            return List.of();
        }
        int maxAdvanceDays = slots.stream()
                .mapToInt(slot -> Math.max(0, slot.getAdvanceDays()))
                .max()
                .orElse(0);
        LocalDate lastDate = today.plusDays(maxAdvanceDays);
        List<DeliverySlotVO> available = new ArrayList<>();
        for (LocalDate date = today; !date.isAfter(lastDate); date = date.plusDays(1)) {
            for (DeliverySlot slot : slots) {
                if (!isDateAllowed(slot, today, date)
                        || !isTimeAllowed(slot, now, date)) {
                    continue;
                }
                int booked = (int) countActiveOrders(slot.getId(), date);
                if (slot.getDailyCapacity() != null && slot.getDailyCapacity() > 0
                        && booked >= slot.getDailyCapacity()) {
                    continue;
                }
                Integer remaining = slot.getDailyCapacity() == null
                        || slot.getDailyCapacity() <= 0
                        ? null
                        : slot.getDailyCapacity() - booked;
                available.add(new DeliverySlotVO(
                        slot.getId(), date, slot.getName(), slot.getStartTime(),
                        slot.getEndTime(), remaining));
            }
        }
        available.sort(Comparator.comparing(DeliverySlotVO::date)
                .thenComparing(DeliverySlotVO::startTime));
        return available;
    }

    @Override
    public DeliverySlot validateAndSnapshot(Long slotId, LocalDate slotDate) {
        if (slotId == null || slotDate == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "请选择配送日期与时段");
        }
        DeliverySlot slot = deliverySlotMapper.selectById(slotId);
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        if (slot == null || slot.getStatus() == null || slot.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "所选配送时段已停用");
        }
        if (!isDateAllowed(slot, today, slotDate) || !isTimeAllowed(slot, now, slotDate)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "所选配送时段不可预约");
        }
        int booked = (int) countActiveOrders(slot.getId(), slotDate);
        if (slot.getDailyCapacity() != null && slot.getDailyCapacity() > 0
                && booked >= slot.getDailyCapacity()) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(),
                    "所选配送时段已满，请选择其他时段");
        }
        return slot;
    }

    private boolean isDateAllowed(DeliverySlot slot, LocalDate today, LocalDate date) {
        if (date.isBefore(today)) {
            return false;
        }
        int advanceDays = Math.max(0, slot.getAdvanceDays() == null ? 0 : slot.getAdvanceDays());
        return !date.isAfter(today.plusDays(advanceDays));
    }

    private boolean isTimeAllowed(DeliverySlot slot, LocalDateTime now, LocalDate date) {
        if (date.isBefore(now.toLocalDate())) {
            return false;
        }
        if (date.equals(now.toLocalDate())
                && !now.toLocalTime().isBefore(slot.getOrderDeadline())) {
            return false;
        }
        return date.isAfter(now.toLocalDate())
                || now.toLocalTime().isBefore(slot.getEndTime());
    }

    private long countActiveOrders(Long slotId, LocalDate date) {
        return orderMapper.selectCount(Wrappers.<Order>lambdaQuery()
                .eq(Order::getDeliverySlotId, slotId)
                .eq(Order::getDeliverySlotDate, date)
                .between(Order::getStatus, STATUS_ACTIVE_FROM, STATUS_ACTIVE_TO));
    }
}
