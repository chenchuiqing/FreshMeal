package com.freshmeal.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freshmeal.common.PageResult;
import com.freshmeal.dto.admin.DispatchLogQuery;
import com.freshmeal.entity.DeliveryManLog;
import com.freshmeal.entity.DeliveryOrder;
import com.freshmeal.mapper.DeliveryManLogMapper;
import com.freshmeal.mapper.DeliveryOrderMapper;
import com.freshmeal.service.AdminDispatchLogService;
import com.freshmeal.vo.admin.AdminDispatchLogVO;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminDispatchLogServiceImpl implements AdminDispatchLogService {

    private final DeliveryManLogMapper deliveryManLogMapper;
    private final DeliveryOrderMapper deliveryOrderMapper;

    @Override
    public PageResult<AdminDispatchLogVO> page(DispatchLogQuery query) {
        List<Long> deliveryIds = null;
        if (query.getDeliveryNo() != null && !query.getDeliveryNo().isBlank()) {
            deliveryIds = deliveryOrderMapper.selectList(
                            Wrappers.<DeliveryOrder>lambdaQuery()
                                    .like(DeliveryOrder::getDeliveryNo,
                                            query.getDeliveryNo().trim()))
                    .stream()
                    .map(DeliveryOrder::getId)
                    .toList();
            if (deliveryIds.isEmpty()) {
                return PageResult.of(List.of(), 0,
                        query.getCurrent(), query.getSize());
            }
        }
        Page<DeliveryManLog> page = deliveryManLogMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                Wrappers.<DeliveryManLog>lambdaQuery()
                        .in(deliveryIds != null,
                                DeliveryManLog::getDeliveryOrderId, deliveryIds)
                        .eq(query.getDeliveryOrderId() != null,
                                DeliveryManLog::getDeliveryOrderId,
                                query.getDeliveryOrderId())
                        .eq(query.getCandidateId() != null,
                                DeliveryManLog::getCandidateId, query.getCandidateId())
                        .eq(query.getIsFinal() != null,
                                DeliveryManLog::getIsFinal, query.getIsFinal())
                        .orderByDesc(DeliveryManLog::getCreateTime)
                        .orderByDesc(DeliveryManLog::getId));
        List<DeliveryManLog> logs = page.getRecords();
        Map<Long, DeliveryOrder> deliveries = loadDeliveries(logs);
        List<AdminDispatchLogVO> records = logs.stream()
                .map(log -> toVo(log, deliveries.get(log.getDeliveryOrderId())))
                .toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private Map<Long, DeliveryOrder> loadDeliveries(List<DeliveryManLog> logs) {
        List<Long> ids = logs.stream()
                .map(DeliveryManLog::getDeliveryOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return deliveryOrderMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(DeliveryOrder::getId, Function.identity()));
    }

    private AdminDispatchLogVO toVo(DeliveryManLog log, DeliveryOrder delivery) {
        return new AdminDispatchLogVO(
                log.getId(),
                log.getDeliveryOrderId(),
                delivery == null ? null : delivery.getDeliveryNo(),
                delivery == null ? null : delivery.getOrderNo(),
                log.getCandidateId(),
                log.getCandidateName(),
                log.getTotalScore(),
                log.getDistanceScore(),
                log.getLoadScore(),
                log.getRatingScore(),
                log.getDistanceValue(),
                log.getLoadValue(),
                log.getIsFinal(),
                log.getCreateTime()
        );
    }
}
