package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.freshmeal.common.Result;
import com.freshmeal.dto.rider.RiderWorkStatusRequest;
import com.freshmeal.security.RiderLoginContext;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.DeliveryOrderService;
import com.freshmeal.vo.rider.RiderDeliveryVO;
import com.freshmeal.vo.rider.RiderStatisticsVO;
import com.freshmeal.vo.rider.RiderWorkStatusVO;
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
@RequestMapping("/api/rider")
@RequiredArgsConstructor
@SaCheckLogin(type = StpInterfaceImpl.RIDER_LOGIN_TYPE)
public class RiderTaskController {

    private final DeliveryOrderService deliveryOrderService;

    @GetMapping("/task/todo")
    public Result<List<RiderDeliveryVO>> todo() {
        return Result.ok(deliveryOrderService.todo(
                RiderLoginContext.currentRiderId()));
    }

    @GetMapping("/task/my")
    public Result<List<RiderDeliveryVO>> myTasks() {
        return Result.ok(deliveryOrderService.myTasks(RiderLoginContext.currentRiderId()));
    }

    @PostMapping("/task/{deliveryNo}/grab")
    public Result<RiderDeliveryVO> grab(@PathVariable String deliveryNo) {
        return Result.ok(deliveryOrderService.grab(
                RiderLoginContext.currentRiderId(), deliveryNo));
    }

    @PutMapping("/task/{deliveryNo}/pickup")
    public Result<RiderDeliveryVO> pickup(@PathVariable String deliveryNo) {
        return Result.ok(deliveryOrderService.pickup(
                RiderLoginContext.currentRiderId(), deliveryNo));
    }

    @PutMapping("/task/{deliveryNo}/deliver")
    public Result<RiderDeliveryVO> deliver(@PathVariable String deliveryNo) {
        return Result.ok(deliveryOrderService.deliver(
                RiderLoginContext.currentRiderId(), deliveryNo));
    }

    @GetMapping("/task/statistics")
    public Result<RiderStatisticsVO> statistics() {
        return Result.ok(deliveryOrderService.statistics(
                RiderLoginContext.currentRiderId()));
    }

    @PutMapping("/profile/work-status")
    public Result<RiderWorkStatusVO> updateWorkStatus(
            @Valid @RequestBody RiderWorkStatusRequest request) {
        return Result.ok(deliveryOrderService.updateWorkStatus(
                RiderLoginContext.currentRiderId(),
                request.onlineStatus(),
                request.workStatus()));
    }
}
