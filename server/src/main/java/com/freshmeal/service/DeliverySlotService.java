package com.freshmeal.service;

import com.freshmeal.entity.DeliverySlot;
import com.freshmeal.vo.user.DeliverySlotVO;
import java.time.LocalDate;
import java.util.List;

public interface DeliverySlotService {

    List<DeliverySlotVO> listAvailable();

    DeliverySlot validateAndSnapshot(Long slotId, LocalDate slotDate);
}
