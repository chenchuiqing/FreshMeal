package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.freshmeal.common.PageResult;
import com.freshmeal.common.Result;
import com.freshmeal.dto.admin.AdminDeliveryQuery;
import com.freshmeal.dto.admin.DeliveryAssignRequest;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.DeliveryOrderService;
import com.freshmeal.vo.admin.AdminDeliveryVO;
import com.freshmeal.vo.admin.AdminRiderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/admin/delivery/order")
@RequiredArgsConstructor
@SaCheckRole(StpInterfaceImpl.ROLE_ADMIN)
public class AdminDeliveryController {

    private final DeliveryOrderService deliveryOrderService;

    @GetMapping("/man")
    public Result<List<AdminRiderVO>> riders() {
        return Result.ok(deliveryOrderService.adminRiders());
    }

    @GetMapping("/page")
    public Result<PageResult<AdminDeliveryVO>> page(@Valid AdminDeliveryQuery query) {
        return Result.ok(deliveryOrderService.adminPage(query));
    }

    @PutMapping("/{id}/assign")
    public Result<AdminDeliveryVO> assign(
            @PathVariable Long id,
            @Valid @RequestBody DeliveryAssignRequest request) {
        return Result.ok(deliveryOrderService.adminAssign(id, request.riderId()));
    }
}
