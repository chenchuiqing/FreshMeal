package com.freshmeal.controller;

import com.freshmeal.common.Result;
import com.freshmeal.dto.rider.RiderAuthRequest;
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
    public Result<Void> logout() {
        riderAuthService.logout();
        return Result.ok();
    }
}
