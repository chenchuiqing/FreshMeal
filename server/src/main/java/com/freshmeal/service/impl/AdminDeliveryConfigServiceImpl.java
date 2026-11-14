package com.freshmeal.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.freshmeal.common.BusinessException;
import com.freshmeal.dto.admin.DeliveryAreaSaveRequest;
import com.freshmeal.dto.admin.DeliverySlotSaveRequest;
import com.freshmeal.entity.DeliveryArea;
import com.freshmeal.entity.DeliverySlot;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.mapper.DeliveryAreaMapper;
import com.freshmeal.mapper.DeliverySlotMapper;
import com.freshmeal.service.AdminDeliveryConfigService;
import com.freshmeal.vo.admin.AdminDeliveryAreaVO;
import com.freshmeal.vo.admin.AdminDeliverySlotVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDeliveryConfigServiceImpl implements AdminDeliveryConfigService {

    private final DeliveryAreaMapper deliveryAreaMapper;
    private final DeliverySlotMapper deliverySlotMapper;

    @Override
    public List<AdminDeliveryAreaVO> areas() {
        return deliveryAreaMapper.selectList(Wrappers.<DeliveryArea>lambdaQuery()
                        .orderByAsc(DeliveryArea::getStatus)
                        .orderByAsc(DeliveryArea::getSort)
                        .orderByAsc(DeliveryArea::getId))
                .stream()
                .map(this::toAreaVo)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminDeliveryAreaVO createArea(DeliveryAreaSaveRequest request) {
        DeliveryArea area = new DeliveryArea();
        applyArea(area, request);
        deliveryAreaMapper.insert(area);
        return toAreaVo(area);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminDeliveryAreaVO updateArea(Long id, DeliveryAreaSaveRequest request) {
        DeliveryArea area = deliveryAreaMapper.selectById(id);
        if (area == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "配送区域不存在");
        }
        applyArea(area, request);
        deliveryAreaMapper.updateById(area);
        return toAreaVo(deliveryAreaMapper.selectById(id));
    }

    @Override
    public List<AdminDeliverySlotVO> slots() {
        return deliverySlotMapper.selectList(Wrappers.<DeliverySlot>lambdaQuery()
                        .orderByAsc(DeliverySlot::getStatus)
                        .orderByAsc(DeliverySlot::getSort)
                        .orderByAsc(DeliverySlot::getStartTime))
                .stream()
                .map(this::toSlotVo)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminDeliverySlotVO createSlot(DeliverySlotSaveRequest request) {
        validateSlot(request);
        DeliverySlot slot = new DeliverySlot();
        applySlot(slot, request);
        deliverySlotMapper.insert(slot);
        return toSlotVo(slot);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminDeliverySlotVO updateSlot(Long id, DeliverySlotSaveRequest request) {
        DeliverySlot slot = deliverySlotMapper.selectById(id);
        if (slot == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "配送时段不存在");
        }
        validateSlot(request);
        applySlot(slot, request);
        deliverySlotMapper.updateById(slot);
        return toSlotVo(deliverySlotMapper.selectById(id));
    }

    private void applyArea(DeliveryArea area, DeliveryAreaSaveRequest request) {
        String district = request.district() == null || request.district().isBlank()
                ? null : request.district().trim();
        area.setName(request.name().trim());
        area.setCity(request.city().trim());
        area.setDistrict(district);
        area.setBaseFee(request.baseFee());
        area.setPerKmFee(request.perKmFee());
        area.setFreeThreshold(request.freeThreshold());
        area.setNightSurcharge(request.nightSurcharge());
        area.setSort(request.sort());
        area.setStatus(request.status());
    }

    private void applySlot(DeliverySlot slot, DeliverySlotSaveRequest request) {
        slot.setName(request.name().trim());
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setOrderDeadline(request.orderDeadline());
        slot.setDailyCapacity(request.dailyCapacity());
        slot.setAdvanceDays(request.advanceDays());
        slot.setSort(request.sort());
        slot.setStatus(request.status());
    }

    private void validateSlot(DeliverySlotSaveRequest request) {
        if (!request.startTime().isBefore(request.endTime())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "配送时段开始时间必须早于结束时间");
        }
        if (!request.orderDeadline().isBefore(request.startTime())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "下单截止时间必须早于配送时段开始时间");
        }
    }

    private AdminDeliveryAreaVO toAreaVo(DeliveryArea area) {
        return new AdminDeliveryAreaVO(
                area.getId(), area.getName(), area.getCity(), area.getDistrict(),
                area.getBaseFee(), area.getPerKmFee(), area.getFreeThreshold(),
                area.getNightSurcharge(), area.getSort(), area.getStatus(),
                area.getCreateTime(), area.getUpdateTime());
    }

    private AdminDeliverySlotVO toSlotVo(DeliverySlot slot) {
        return new AdminDeliverySlotVO(
                slot.getId(), slot.getName(), slot.getStartTime(), slot.getEndTime(),
                slot.getOrderDeadline(), slot.getDailyCapacity(), slot.getAdvanceDays(),
                slot.getSort(), slot.getStatus(), slot.getCreateTime(), slot.getUpdateTime());
    }
}
