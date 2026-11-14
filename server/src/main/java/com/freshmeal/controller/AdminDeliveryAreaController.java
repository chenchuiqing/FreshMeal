package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.freshmeal.common.Result;
import com.freshmeal.dto.admin.DeliveryAreaSaveRequest;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.AdminDeliveryConfigService;
import com.freshmeal.vo.admin.AdminDeliveryAreaVO;
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
@RequestMapping("/api/admin/delivery/area")
@RequiredArgsConstructor
@SaCheckRole(StpInterfaceImpl.ROLE_ADMIN)
public class AdminDeliveryAreaController {

    private final AdminDeliveryConfigService configService;

    @GetMapping
    public Result<List<AdminDeliveryAreaVO>> list() {
        return Result.ok(configService.areas());
    }

    @PostMapping
    public Result<AdminDeliveryAreaVO> create(
            @Valid @RequestBody DeliveryAreaSaveRequest request) {
        return Result.ok(configService.createArea(request));
    }

    @PutMapping("/{id}")
    public Result<AdminDeliveryAreaVO> update(
            @PathVariable Long id,
            @Valid @RequestBody DeliveryAreaSaveRequest request) {
        return Result.ok(configService.updateArea(id, request));
    }
}
