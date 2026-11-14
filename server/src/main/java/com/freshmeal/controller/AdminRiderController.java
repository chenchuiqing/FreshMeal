package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.freshmeal.common.PageResult;
import com.freshmeal.common.Result;
import com.freshmeal.dto.admin.AdminRiderCreateRequest;
import com.freshmeal.dto.admin.AdminRiderQuery;
import com.freshmeal.dto.admin.AdminRiderUpdateRequest;
import com.freshmeal.dto.admin.AdminRiderWorkRequest;
import com.freshmeal.dto.admin.StatusRequest;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.AdminRiderService;
import com.freshmeal.vo.admin.AdminRiderManageVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/delivery/man")
@RequiredArgsConstructor
@SaCheckRole(StpInterfaceImpl.ROLE_ADMIN)
public class AdminRiderController {

    private final AdminRiderService adminRiderService;

    @GetMapping
    public Result<List<AdminRiderManageVO>> list() {
        return Result.ok(adminRiderService.list());
    }

    @GetMapping("/page")
    public Result<PageResult<AdminRiderManageVO>> page(@Valid AdminRiderQuery query) {
        return Result.ok(adminRiderService.page(query));
    }

    @PostMapping
    public Result<AdminRiderManageVO> create(
            @Valid @RequestBody AdminRiderCreateRequest request) {
        return Result.ok(adminRiderService.create(request));
    }

    @PutMapping("/{id}")
    public Result<AdminRiderManageVO> update(
            @PathVariable Long id,
            @Valid @RequestBody AdminRiderUpdateRequest request) {
        return Result.ok(adminRiderService.update(id, request));
    }

    @PutMapping("/{id}/status")
    public Result<AdminRiderManageVO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequest request) {
        return Result.ok(adminRiderService.updateStatus(id, request.status()));
    }

    @PutMapping("/{id}/work")
    public Result<AdminRiderManageVO> updateWorkStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminRiderWorkRequest request) {
        return Result.ok(adminRiderService.updateWorkStatus(
                id, request.onlineStatus(), request.workStatus()));
    }
}
