package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.freshmeal.common.Result;
import com.freshmeal.dto.user.UserAuthRequest;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.UserAuthService;
import com.freshmeal.vo.user.UserLoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;

    @PostMapping("/register")
    public Result<UserLoginVO> register(@Valid @RequestBody UserAuthRequest request) {
        return Result.ok(userAuthService.register(request));
    }

    @PostMapping("/login")
    public Result<UserLoginVO> login(@Valid @RequestBody UserAuthRequest request) {
        return Result.ok(userAuthService.login(request));
    }

    @PostMapping("/logout")
    @SaCheckLogin(type = StpInterfaceImpl.USER_LOGIN_TYPE)
    public Result<Void> logout() {
        userAuthService.logout();
        return Result.ok();
    }
}
