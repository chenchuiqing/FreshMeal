package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.freshmeal.common.Result;
import com.freshmeal.dto.admin.AdminLoginRequest;
import com.freshmeal.service.AdminAuthService;
import com.freshmeal.vo.admin.AdminLoginVO;
import com.freshmeal.vo.admin.AdminProfileVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginRequest request) {
        return Result.ok(adminAuthService.login(request));
    }

    @GetMapping("/profile")
    @SaCheckLogin
    public Result<AdminProfileVO> profile() {
        return Result.ok(adminAuthService.getProfile());
    }

    @PostMapping("/logout")
    @SaCheckLogin
    public Result<Void> logout() {
        adminAuthService.logout();
        return Result.ok();
    }
}
