package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.freshmeal.common.PageResult;
import com.freshmeal.common.Result;
import com.freshmeal.dto.user.OrderCreateRequest;
import com.freshmeal.dto.user.OrderQuery;
import com.freshmeal.dto.user.PaymentInitiateRequest;
import com.freshmeal.security.ShopLoginContext;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.OrderService;
import com.freshmeal.service.PaymentService;
import com.freshmeal.vo.user.OrderVO;
import com.freshmeal.vo.user.PaymentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop/order")
@RequiredArgsConstructor
@SaCheckLogin(type = StpInterfaceImpl.USER_LOGIN_TYPE)
public class ShopOrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @PostMapping
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateRequest request) {
        return Result.ok(orderService.create(ShopLoginContext.currentUserId(), request));
    }

    @GetMapping("/page")
    public Result<PageResult<OrderVO>> page(@Valid OrderQuery query) {
        return Result.ok(orderService.page(ShopLoginContext.currentUserId(), query));
    }

    @GetMapping("/{orderNo}")
    public Result<OrderVO> detail(@PathVariable String orderNo) {
        return Result.ok(orderService.detail(ShopLoginContext.currentUserId(), orderNo));
    }

    @DeleteMapping("/{orderNo}")
    public Result<Void> cancel(@PathVariable String orderNo,
            @RequestBody(required = false) String reason) {
        orderService.cancel(ShopLoginContext.currentUserId(), orderNo, reason);
        return Result.ok();
    }

    @PostMapping("/{orderNo}/pay")
    public Result<PaymentVO> initiatePay(@PathVariable String orderNo,
            @Valid @RequestBody PaymentInitiateRequest request) {
        return Result.ok(paymentService.initiate(
                ShopLoginContext.currentUserId(), orderNo, request.payType()));
    }
}
