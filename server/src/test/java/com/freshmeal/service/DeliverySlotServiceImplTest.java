package com.freshmeal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.freshmeal.common.BusinessException;
import com.freshmeal.entity.DeliverySlot;
import com.freshmeal.entity.Order;
import com.freshmeal.mapper.DeliverySlotMapper;
import com.freshmeal.mapper.OrderMapper;
import com.freshmeal.service.impl.DeliverySlotServiceImpl;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliverySlotServiceImplTest {

    @Mock
    private DeliverySlotMapper deliverySlotMapper;

    @Mock
    private OrderMapper orderMapper;

    @Test
    void expiredDeadlineRejectsReservation() {
        DeliverySlot slot = slot(1L, LocalDate.now(), LocalTime.now().minusMinutes(1), 0);
        when(deliverySlotMapper.selectById(1L)).thenReturn(slot);
        DeliverySlotServiceImpl service =
                new DeliverySlotServiceImpl(deliverySlotMapper, orderMapper);

        assertThatThrownBy(() -> service.validateAndSnapshot(1L, LocalDate.now()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("所选配送时段不可预约");
    }

    @Test
    void fullCapacityRejectsReservation() {
        DeliverySlot slot = slot(2L, LocalDate.now().plusDays(1),
                LocalTime.MAX, 1);
        when(deliverySlotMapper.selectById(2L)).thenReturn(slot);
        when(orderMapper.selectCount(org.mockito.ArgumentMatchers.any()))
                .thenReturn(1L);
        DeliverySlotServiceImpl service =
                new DeliverySlotServiceImpl(deliverySlotMapper, orderMapper);

        assertThatThrownBy(() ->
                service.validateAndSnapshot(2L, LocalDate.now().plusDays(1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("所选配送时段已满，请选择其他时段");
    }

    @Test
    void availableSlotsExposeRemainingCapacity() {
        DeliverySlot slot = slot(3L, LocalDate.now().plusDays(1),
                LocalTime.NOON, 5);
        when(deliverySlotMapper.selectList(
                org.mockito.ArgumentMatchers.any())).thenReturn(java.util.List.of(slot));
        when(orderMapper.selectCount(org.mockito.ArgumentMatchers.any()))
                .thenReturn(2L);
        DeliverySlotServiceImpl service =
                new DeliverySlotServiceImpl(deliverySlotMapper, orderMapper);

        assertThat(service.listAvailable())
                .singleElement()
                .satisfies(available -> {
                    assertThat(available.id()).isEqualTo(3L);
                    assertThat(available.date()).isEqualTo(LocalDate.now().plusDays(1));
                    assertThat(available.remainingCapacity()).isEqualTo(3);
                });
    }

    private DeliverySlot slot(Long id, LocalDate date, LocalTime deadline,
            int capacity) {
        DeliverySlot slot = new DeliverySlot();
        slot.setId(id);
        slot.setName("测试时段");
        slot.setStartTime(LocalTime.of(11, 0));
        slot.setEndTime(LocalTime.of(13, 0));
        slot.setOrderDeadline(date.equals(LocalDate.now())
                ? deadline : LocalTime.of(10, 30));
        slot.setDailyCapacity(capacity);
        slot.setAdvanceDays(1);
        slot.setSort(1);
        slot.setStatus(1);
        return slot;
    }
}
