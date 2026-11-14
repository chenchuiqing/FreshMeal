package com.freshmeal.vo.user;

import java.math.BigDecimal;

public record AddressVO(
        Long id,
        String receiver,
        String phone,
        String province,
        String city,
        String district,
        String detail,
        String fullAddress,
        BigDecimal longitude,
        BigDecimal latitude,
        String tag,
        Boolean isDefault
) {
}
