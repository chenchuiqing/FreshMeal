package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.freshmeal.common.Result;
import com.freshmeal.dto.admin.DeliverySlotSaveRequest;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.AdminDeliveryConfigService;
import com.freshmeal.vo.admin.AdminDeliverySlotVO;
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
@RequestMapping("/api/admin/delivery/slot")
@RequiredArgsConstructor
@SaCheckRole(StpInterfaceImpl.ROLE_ADMIN)
public class AdminDeliverySlotController {

    private final AdminDeliveryConfigService configService;

    @GetMapping
    public Result<List<AdminDeliverySlotVO>> list() {
        return Result.ok(configService.slots());
    }

    @PostMapping
    public Result<AdminDeliverySlotVO> create(
            @Valid @RequestBody DeliverySlotSaveRequest request) {
        return Result.ok(configService.createSlot(request));
    }

    @PutMapping("/{id}")
    public Result<AdminDeliverySlotVO> update(
            @PathVariable Long id,
            @Valid @RequestBody DeliverySlotSaveRequest request) {
        return Result.ok(configService.updateSlot(id, request));
    }
}
