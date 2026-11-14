package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.freshmeal.common.Result;
import com.freshmeal.dto.user.RefundRequest;
import com.freshmeal.security.ShopLoginContext;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.RefundService;
import com.freshmeal.vo.user.RefundVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop/refund")
@RequiredArgsConstructor
@SaCheckLogin(type = StpInterfaceImpl.USER_LOGIN_TYPE)
public class ShopRefundController {

    private final RefundService refundService;

    @GetMapping("/list")
    public Result<List<RefundVO>> list() {
        return Result.ok(refundService.listUser(
                ShopLoginContext.currentUserId()));
    }

    @PostMapping("/{orderNo}")
    public Result<Void> request(@PathVariable String orderNo,
            @Valid @RequestBody RefundRequest request) {
        refundService.request(ShopLoginContext.currentUserId(), orderNo,
                request.reason());
        return Result.ok();
    }
}
