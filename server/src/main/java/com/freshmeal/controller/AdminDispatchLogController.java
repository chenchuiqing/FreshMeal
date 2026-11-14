package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.freshmeal.common.PageResult;
import com.freshmeal.common.Result;
import com.freshmeal.dto.admin.DispatchLogQuery;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.AdminDispatchLogService;
import com.freshmeal.vo.admin.AdminDispatchLogVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dispatch/log")
@RequiredArgsConstructor
@SaCheckRole(StpInterfaceImpl.ROLE_ADMIN)
public class AdminDispatchLogController {

    private final AdminDispatchLogService dispatchLogService;

    @GetMapping("/page")
    public Result<PageResult<AdminDispatchLogVO>> page(
            @Valid DispatchLogQuery query) {
        return Result.ok(dispatchLogService.page(query));
    }
}
