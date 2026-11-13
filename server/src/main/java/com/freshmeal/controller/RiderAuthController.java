package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.freshmeal.common.Result;
import com.freshmeal.dto.rider.RiderAuthRequest;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.RiderAuthService;
import com.freshmeal.vo.rider.RiderLoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rider/auth")
@RequiredArgsConstructor
public class RiderAuthController {

    private final RiderAuthService riderAuthService;

    @PostMapping("/login")
    public Result<RiderLoginVO> login(@Valid @RequestBody RiderAuthRequest request) {
        return Result.ok(riderAuthService.login(request));
    }

    @PostMapping("/logout")
    @SaCheckLogin(type = StpInterfaceImpl.RIDER_LOGIN_TYPE)
    public Result<Void> logout() {
        riderAuthService.logout();
        return Result.ok();
    }
}
