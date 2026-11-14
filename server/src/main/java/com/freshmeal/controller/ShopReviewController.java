package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.freshmeal.common.Result;
import com.freshmeal.dto.user.ReviewSubmitRequest;
import com.freshmeal.security.ShopLoginContext;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.ReviewService;
import com.freshmeal.vo.user.ReviewVO;
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
@RequestMapping("/api/shop/order")
@RequiredArgsConstructor
@SaCheckLogin(type = StpInterfaceImpl.USER_LOGIN_TYPE)
public class ShopReviewController {

    private final ReviewService reviewService;

    @PostMapping("/{orderNo}/review")
    public Result<ReviewVO> submit(@PathVariable String orderNo,
            @Valid @RequestBody ReviewSubmitRequest request) {
        return Result.ok(reviewService.submit(
                ShopLoginContext.currentUserId(), orderNo, request));
    }

    @GetMapping("/{orderNo}/review")
    public Result<List<ReviewVO>> list(@PathVariable String orderNo) {
        return Result.ok(reviewService.listByOrder(
                ShopLoginContext.currentUserId(), orderNo));
    }
}
