package com.freshmeal.service.impl;

import com.freshmeal.entity.DeliveryArea;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class FreightRuleCalculator {

    private static final BigDecimal FIRST_KM = BigDecimal.ONE;

    public BigDecimal calculate(DeliveryArea area, BigDecimal distance,
            BigDecimal goodsAmount, boolean nightTime) {
        BigDecimal safeDistance = distance == null ? BigDecimal.ZERO : distance;
        BigDecimal freight = area.getBaseFee();
        BigDecimal extraDistance = safeDistance.subtract(FIRST_KM).max(BigDecimal.ZERO);
        freight = freight.add(extraDistance.multiply(area.getPerKmFee()));
        if (nightTime) {
            freight = freight.add(area.getNightSurcharge());
        }
        if (area.getFreeThreshold().compareTo(BigDecimal.ZERO) > 0
                && goodsAmount.compareTo(area.getFreeThreshold()) >= 0) {
            freight = BigDecimal.ZERO;
        }
        return freight.setScale(2, RoundingMode.HALF_UP);
    }
}
