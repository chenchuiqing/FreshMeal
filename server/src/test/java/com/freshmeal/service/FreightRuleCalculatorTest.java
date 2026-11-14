package com.freshmeal.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.freshmeal.entity.DeliveryArea;
import com.freshmeal.service.impl.FreightRuleCalculator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FreightRuleCalculatorTest {

    private final FreightRuleCalculator calculator = new FreightRuleCalculator();

    @Test
    void calculatesBaseMileageNightAndFreeShippingRules() {
        DeliveryArea area = area();

        assertThat(calculator.calculate(
                area, new BigDecimal("1.00"), BigDecimal.ZERO, false))
                .isEqualByComparingTo("5.00");
        assertThat(calculator.calculate(
                area, new BigDecimal("3.00"), BigDecimal.ZERO, false))
                .isEqualByComparingTo("9.00");
        assertThat(calculator.calculate(
                area, new BigDecimal("3.00"), BigDecimal.ZERO, true))
                .isEqualByComparingTo("15.00");
        assertThat(calculator.calculate(
                area, new BigDecimal("8.00"), new BigDecimal("80.00"), true))
                .isEqualByComparingTo("0.00");
        assertThat(calculator.calculate(
                area, new BigDecimal("8.00"), new BigDecimal("79.99"), false))
                .isEqualByComparingTo("19.00");
    }

    private DeliveryArea area() {
        DeliveryArea area = new DeliveryArea();
        area.setBaseFee(new BigDecimal("5.00"));
        area.setPerKmFee(new BigDecimal("2.00"));
        area.setNightSurcharge(new BigDecimal("6.00"));
        area.setFreeThreshold(new BigDecimal("80.00"));
        return area;
    }
}
