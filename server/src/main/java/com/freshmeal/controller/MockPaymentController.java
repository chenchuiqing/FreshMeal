package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.freshmeal.common.Result;
import com.freshmeal.dto.user.MockPayNotifyRequest;
import com.freshmeal.security.ShopLoginContext;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.PaymentService;
import com.freshmeal.vo.user.PaymentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pay/mock")
@RequiredArgsConstructor
@SaCheckLogin(type = StpInterfaceImpl.USER_LOGIN_TYPE)
public class MockPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/notify")
    public Result<PaymentVO> notify(@Valid @RequestBody MockPayNotifyRequest request) {
        return Result.ok(paymentService.mockNotify(
                ShopLoginContext.currentUserId(), request));
    }
}
