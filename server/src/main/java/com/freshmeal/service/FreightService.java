package com.freshmeal.service;

import com.freshmeal.entity.Address;
import com.freshmeal.vo.user.FreightVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface FreightService {

    FreightVO calculate(Address address, BigDecimal goodsAmount, LocalDateTime pricedAt);
}
