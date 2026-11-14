package com.freshmeal.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.freshmeal.common.BusinessException;
import com.freshmeal.entity.Address;
import com.freshmeal.entity.DeliveryArea;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.mapper.DeliveryAreaMapper;
import com.freshmeal.service.FreightService;
import com.freshmeal.vo.user.FreightVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FreightServiceImpl implements FreightService {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private final DeliveryAreaMapper deliveryAreaMapper;
    private final FreightRuleCalculator freightRuleCalculator;

    @Value("${freshmeal.store.longitude:121.473701}")
    private BigDecimal storeLongitude;

    @Value("${freshmeal.store.latitude:31.230416}")
    private BigDecimal storeLatitude;

    @Override
    public FreightVO calculate(Address address, BigDecimal goodsAmount, LocalDateTime pricedAt) {
        if (address == null) {
            throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND.getCode(),
                    ErrorCode.ADDRESS_NOT_FOUND.getMessage());
        }
        DeliveryArea area = findArea(address);
        BigDecimal distance = calculateDistance(address);
        boolean night = pricedAt.toLocalTime().isAfter(LocalTime.of(21, 59, 59))
                || pricedAt.toLocalTime().isBefore(LocalTime.of(6, 0));
        BigDecimal freight = freightRuleCalculator.calculate(area, distance, goodsAmount, night);
        boolean freeShipping = freight.signum() == 0
                && area.getFreeThreshold().compareTo(goodsAmount) <= 0;
        return new FreightVO(
                area.getId(),
                area.getName(),
                distance,
                freight,
                freeShipping
        );
    }

    private DeliveryArea findArea(Address address) {
        DeliveryArea districtArea = deliveryAreaMapper.selectOne(
                Wrappers.<DeliveryArea>lambdaQuery()
                        .eq(DeliveryArea::getStatus, 1)
                        .eq(DeliveryArea::getCity, address.getCity())
                        .eq(DeliveryArea::getDistrict, address.getDistrict())
                        .orderByAsc(DeliveryArea::getSort)
                        .last("LIMIT 1"));
        if (districtArea != null) {
            return districtArea;
        }

        DeliveryArea cityArea = deliveryAreaMapper.selectOne(
                Wrappers.<DeliveryArea>lambdaQuery()
                        .eq(DeliveryArea::getStatus, 1)
                        .eq(DeliveryArea::getCity, address.getCity())
                        .isNull(DeliveryArea::getDistrict)
                        .orderByAsc(DeliveryArea::getSort)
                        .last("LIMIT 1"));
        if (cityArea != null) {
            return cityArea;
        }
        throw new BusinessException(ErrorCode.DELIVERY_AREA_NOT_FOUND.getCode(),
                ErrorCode.DELIVERY_AREA_NOT_FOUND.getMessage());
    }

    private BigDecimal calculateDistance(Address address) {
        if (address.getLongitude() == null || address.getLatitude() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        double latitude1 = Math.toRadians(storeLatitude.doubleValue());
        double latitude2 = Math.toRadians(address.getLatitude().doubleValue());
        double deltaLatitude = latitude2 - latitude1;
        double deltaLongitude = Math.toRadians(
                address.getLongitude().doubleValue() - storeLongitude.doubleValue());
        double sinLatitude = Math.sin(deltaLatitude / 2);
        double sinLongitude = Math.sin(deltaLongitude / 2);
        double value = sinLatitude * sinLatitude
                + Math.cos(latitude1) * Math.cos(latitude2) * sinLongitude * sinLongitude;
        double centralAngle = 2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
        return BigDecimal.valueOf(EARTH_RADIUS_KM * centralAngle)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
