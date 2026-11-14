package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.freshmeal.common.PageResult;
import com.freshmeal.common.Result;
import com.freshmeal.dto.admin.CouponIssueRequest;
import com.freshmeal.dto.admin.CouponSaveRequest;
import com.freshmeal.dto.admin.StatusRequest;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.CouponService;
import com.freshmeal.vo.admin.AdminCouponVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/coupon")
@RequiredArgsConstructor
@SaCheckRole(StpInterfaceImpl.ROLE_ADMIN)
public class AdminCouponController {

    private final CouponService couponService;

    @GetMapping("/page")
    public Result<PageResult<AdminCouponVO>> page() {
        return Result.ok(couponService.pageAdmin());
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody CouponSaveRequest request) {
        return Result.ok(couponService.create(request));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
            @Valid @RequestBody StatusRequest body) {
        couponService.updateStatus(id, body.status());
        return Result.ok();
    }

    @PostMapping("/{id}/issue")
    public Result<Void> issue(@PathVariable Long id,
            @Valid @RequestBody CouponIssueRequest request) {
        couponService.issue(id, request.userId());
        return Result.ok();
    }
}
