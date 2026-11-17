package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.freshmeal.common.Result;
import com.freshmeal.security.ShopLoginContext;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.CouponService;
import com.freshmeal.vo.user.CouponVO;
import com.freshmeal.vo.user.UserCouponVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop/coupon")
@RequiredArgsConstructor
@SaCheckLogin(type = StpInterfaceImpl.USER_LOGIN_TYPE)
public class ShopCouponController {

    private final CouponService couponService;

    @GetMapping("/available")
    public Result<List<CouponVO>> available() {
        return Result.ok(couponService.listAvailable(ShopLoginContext.currentUserId()));
    }

    @GetMapping("/list")
    public Result<List<UserCouponVO>> list() {
        return Result.ok(couponService.listUserCoupons(
                ShopLoginContext.currentUserId()));
    }

    @PostMapping("/{id}/receive")
    public Result<UserCouponVO> receive(@PathVariable Long id) {
        return Result.ok(couponService.receive(
                ShopLoginContext.currentUserId(), id));
    }
}
