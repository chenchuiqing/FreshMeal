package com.freshmeal.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freshmeal.common.BusinessException;
import com.freshmeal.common.PageResult;
import com.freshmeal.dto.admin.AdminRiderCreateRequest;
import com.freshmeal.dto.admin.AdminRiderQuery;
import com.freshmeal.dto.admin.AdminRiderUpdateRequest;
import com.freshmeal.entity.DeliveryArea;
import com.freshmeal.entity.DeliveryMan;
import com.freshmeal.entity.DeliveryOrder;
import com.freshmeal.enums.DeliveryStatus;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.mapper.DeliveryAreaMapper;
import com.freshmeal.mapper.DeliveryManMapper;
import com.freshmeal.mapper.DeliveryOrderMapper;
import com.freshmeal.service.AdminRiderService;
import com.freshmeal.vo.admin.AdminRiderManageVO;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminRiderServiceImpl implements AdminRiderService {

    private final DeliveryManMapper deliveryManMapper;
    private final DeliveryAreaMapper deliveryAreaMapper;
    private final DeliveryOrderMapper deliveryOrderMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<AdminRiderManageVO> page(AdminRiderQuery query) {
        Page<DeliveryMan> page = deliveryManMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                Wrappers.<DeliveryMan>lambdaQuery()
                        .eq(query.getStatus() != null,
                                DeliveryMan::getStatus, query.getStatus())
                        .eq(query.getOnlineStatus() != null,
                                DeliveryMan::getOnlineStatus, query.getOnlineStatus())
                        .and(query.getKeyword() != null && !query.getKeyword().isBlank(),
                                wrapper -> {
                                    String keyword = query.getKeyword().trim();
                                    wrapper.like(DeliveryMan::getUsername, keyword)
                                            .or().like(DeliveryMan::getRealName, keyword)
                                            .or().like(DeliveryMan::getPhone, keyword);
                                })
                        .orderByAsc(DeliveryMan::getStatus)
                        .orderByAsc(DeliveryMan::getId));
        List<AdminRiderManageVO> records = toVos(page.getRecords());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<AdminRiderManageVO> list() {
        return toVos(deliveryManMapper.selectList(
                Wrappers.<DeliveryMan>lambdaQuery()
                        .eq(DeliveryMan::getStatus, 1)
                        .orderByAsc(DeliveryMan::getId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminRiderManageVO create(AdminRiderCreateRequest request) {
        requireUsernameAvailable(request.username(), null);
        requireArea(request.serviceAreaId());
        DeliveryMan rider = new DeliveryMan();
        rider.setUsername(request.username().trim());
        rider.setPassword(passwordEncoder.encode(request.password()));
        rider.setRealName(request.realName().trim());
        rider.setPhone(request.phone().trim());
        rider.setMaxOrderNum(request.maxOrderNum());
        rider.setServiceAreaId(request.serviceAreaId());
        rider.setStatus(request.status());
        rider.setOnlineStatus(0);
        rider.setWorkStatus(request.status() == 1 ? 0 : 2);
        deliveryManMapper.insert(rider);
        return toVo(rider);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminRiderManageVO update(Long id, AdminRiderUpdateRequest request) {
        DeliveryMan rider = requireRider(id);
        requireUsernameAvailable(request.username(), id);
        requireArea(request.serviceAreaId());
        if (request.status() == 0 && activeCount(id) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(),
                    "配送员仍有进行中的任务，不能禁用账号");
        }
        rider.setUsername(request.username().trim());
        rider.setRealName(request.realName().trim());
        rider.setPhone(request.phone().trim());
        rider.setMaxOrderNum(request.maxOrderNum());
        rider.setServiceAreaId(request.serviceAreaId());
        rider.setStatus(request.status());
        if (request.password() != null && !request.password().isBlank()) {
            rider.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.status() == 0) {
            rider.setOnlineStatus(0);
            rider.setWorkStatus(2);
        }
        deliveryManMapper.updateById(rider);
        return toVo(requireRider(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminRiderManageVO updateStatus(Long id, Integer status) {
        DeliveryMan rider = requireRider(id);
        if (status == 0 && activeCount(id) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(),
                    "配送员仍有进行中的任务，不能禁用账号");
        }
        rider.setStatus(status);
        if (status == 0) {
            rider.setOnlineStatus(0);
            rider.setWorkStatus(2);
        }
        deliveryManMapper.updateById(rider);
        return toVo(requireRider(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminRiderManageVO updateWorkStatus(
            Long id, Integer onlineStatus, Integer workStatus) {
        DeliveryMan rider = requireRider(id);
        if (rider.getStatus() == null || rider.getStatus() != 1) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(), "禁用账号不能调整工作状态");
        }
        if (onlineStatus == 0 && workStatus != 2) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "离线时工作状态必须为休息");
        }
        if (onlineStatus == 1 && workStatus == 2 && activeCount(id) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(),
                    "仍有配送任务时不能设为休息");
        }
        rider.setOnlineStatus(onlineStatus);
        rider.setWorkStatus(workStatus);
        deliveryManMapper.updateById(rider);
        return toVo(requireRider(id));
    }

    private List<AdminRiderManageVO> toVos(List<DeliveryMan> riders) {
        if (riders.isEmpty()) {
            return List.of();
        }
        List<Long> areaIds = riders.stream()
                .map(DeliveryMan::getServiceAreaId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> areaNames = areaIds.isEmpty() ? Map.of()
                : deliveryAreaMapper.selectBatchIds(areaIds).stream()
                .collect(Collectors.toMap(DeliveryArea::getId, DeliveryArea::getName));
        return riders.stream()
                .map(rider -> toVo(rider, areaNames))
                .toList();
    }

    private AdminRiderManageVO toVo(DeliveryMan rider) {
        String areaName = rider.getServiceAreaId() == null ? null
                : optionalName(rider.getServiceAreaId());
        return toVo(rider, areaName == null ? Map.of()
                : Map.of(rider.getServiceAreaId(), areaName));
    }

    private String optionalName(Long areaId) {
        DeliveryArea area = deliveryAreaMapper.selectById(areaId);
        return area == null ? null : area.getName();
    }

    private AdminRiderManageVO toVo(DeliveryMan rider, Map<Long, String> areaNames) {
        int active = activeCount(rider.getId());
        boolean assignable = rider.getStatus() != null && rider.getStatus() == 1
                && rider.getOnlineStatus() != null && rider.getOnlineStatus() == 1
                && rider.getWorkStatus() != null
                && (rider.getWorkStatus() == 0 || rider.getWorkStatus() == 1)
                && rider.getMaxOrderNum() != null && active < rider.getMaxOrderNum();
        return new AdminRiderManageVO(
                rider.getId(),
                rider.getUsername(),
                rider.getRealName(),
                rider.getPhone(),
                rider.getServiceAreaId(),
                rider.getServiceAreaId() == null ? null
                        : areaNames.get(rider.getServiceAreaId()),
                rider.getOnlineStatus(),
                rider.getWorkStatus(),
                rider.getMaxOrderNum(),
                active,
                // 今日完成按送达时间实时统计，与骑手端 statistics 同口径。
                (int) deliveryOrderMapper.countTodayDelivered(rider.getId()),
                rider.getTotalOrderCount(),
                rider.getTotalIncome(),
                rider.getRating(),
                rider.getStatus(),
                assignable,
                rider.getCreateTime(),
                rider.getUpdateTime()
        );
    }

    private DeliveryMan requireRider(Long id) {
        DeliveryMan rider = deliveryManMapper.selectById(id);
        if (rider == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "配送员不存在");
        }
        return rider;
    }

    private void requireUsernameAvailable(String username, Long currentId) {
        DeliveryMan existing = deliveryManMapper.selectOne(
                Wrappers.<DeliveryMan>lambdaQuery()
                        .eq(DeliveryMan::getUsername, username.trim()));
        if (existing != null && !existing.getId().equals(currentId)) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(), "登录账号已存在");
        }
    }

    private void requireArea(Long areaId) {
        if (areaId != null && deliveryAreaMapper.selectById(areaId) == null) {
            throw new BusinessException(ErrorCode.DELIVERY_AREA_NOT_FOUND.getCode(),
                    "服务区域不存在");
        }
    }

    private int activeCount(Long riderId) {
        return Math.toIntExact(deliveryOrderMapper.selectCount(
                Wrappers.<DeliveryOrder>lambdaQuery()
                        .eq(DeliveryOrder::getDeliveryManId, riderId)
                        .between(DeliveryOrder::getStatus,
                                DeliveryStatus.ASSIGNED.getCode(),
                                DeliveryStatus.DELIVERING.getCode())));
    }
}
