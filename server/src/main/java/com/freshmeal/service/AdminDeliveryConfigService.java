package com.freshmeal.service;

import com.freshmeal.dto.admin.DeliveryAreaSaveRequest;
import com.freshmeal.dto.admin.DeliverySlotSaveRequest;
import com.freshmeal.vo.admin.AdminDeliveryAreaVO;
import com.freshmeal.vo.admin.AdminDeliverySlotVO;
import java.util.List;

public interface AdminDeliveryConfigService {

    List<AdminDeliveryAreaVO> areas();

    AdminDeliveryAreaVO createArea(DeliveryAreaSaveRequest request);

    AdminDeliveryAreaVO updateArea(Long id, DeliveryAreaSaveRequest request);

    List<AdminDeliverySlotVO> slots();

    AdminDeliverySlotVO createSlot(DeliverySlotSaveRequest request);

    AdminDeliverySlotVO updateSlot(Long id, DeliverySlotSaveRequest request);
}
