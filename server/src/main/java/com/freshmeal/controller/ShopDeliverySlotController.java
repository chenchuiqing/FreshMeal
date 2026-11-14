package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.freshmeal.common.Result;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.DeliverySlotService;
import com.freshmeal.vo.user.DeliverySlotVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop/delivery-slot")
@RequiredArgsConstructor
@SaCheckLogin(type = StpInterfaceImpl.USER_LOGIN_TYPE)
public class ShopDeliverySlotController {

    private final DeliverySlotService deliverySlotService;

    @GetMapping("/available")
    public Result<List<DeliverySlotVO>> available() {
        return Result.ok(deliverySlotService.listAvailable());
    }
}
