package com.freshmeal.service;

import com.freshmeal.dto.user.MockPayNotifyRequest;
import com.freshmeal.vo.user.PaymentVO;

public interface PaymentService {

    PaymentVO initiate(Long userId, String orderNo, Integer payType);

    PaymentVO mockNotify(Long userId, MockPayNotifyRequest request);
}
