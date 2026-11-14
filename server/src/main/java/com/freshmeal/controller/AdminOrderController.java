package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.freshmeal.common.PageResult;
import com.freshmeal.common.Result;
import com.freshmeal.dto.admin.AdminOrderQuery;
import com.freshmeal.dto.admin.AdminOrderStatusRequest;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.AdminOrderService;
import com.freshmeal.vo.admin.AdminOrderDetailVO;
import com.freshmeal.vo.admin.AdminOrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/order")
@RequiredArgsConstructor
@SaCheckRole(StpInterfaceImpl.ROLE_ADMIN)
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping("/page")
    public Result<PageResult<AdminOrderVO>> page(@Valid AdminOrderQuery query) {
        return Result.ok(adminOrderService.page(query));
    }

    @GetMapping("/{orderNo}")
    public Result<AdminOrderDetailVO> detail(@PathVariable String orderNo) {
        return Result.ok(adminOrderService.detail(orderNo));
    }

    @PutMapping("/{orderNo}/status")
    public Result<AdminOrderVO> updateStatus(
            @PathVariable String orderNo,
            @Valid @RequestBody AdminOrderStatusRequest request) {
        return Result.ok(adminOrderService.updateStatus(
                orderNo, request.targetStatus(), request.reason(),
                StpUtil.getLoginIdAsLong()));
    }
}
