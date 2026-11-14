package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.freshmeal.common.PageResult;
import com.freshmeal.common.Result;
import com.freshmeal.dto.admin.RefundDecisionRequest;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.RefundService;
import com.freshmeal.vo.admin.AdminRefundVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/refund")
@RequiredArgsConstructor
@SaCheckRole(StpInterfaceImpl.ROLE_ADMIN)
public class AdminRefundController {

    private final RefundService refundService;

    @GetMapping("/page")
    public Result<PageResult<AdminRefundVO>> page(
            @RequestParam(value = "current", defaultValue = "1") int current,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) Integer status) {
        return Result.ok(refundService.pageAdmin(current, size, status));
    }

    @PutMapping("/{id}/decision")
    public Result<Void> decide(@PathVariable Long id,
            @Valid @RequestBody RefundDecisionRequest request) {
        refundService.decide(id, request.status(), request.adminReply(),
                StpUtil.getLoginIdAsLong());
        return Result.ok();
    }
}
