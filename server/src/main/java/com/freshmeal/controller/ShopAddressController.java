package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.freshmeal.common.Result;
import com.freshmeal.dto.user.AddressSaveRequest;
import com.freshmeal.security.ShopLoginContext;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.AddressService;
import com.freshmeal.vo.user.AddressVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop/address")
@RequiredArgsConstructor
@SaCheckLogin(type = StpInterfaceImpl.USER_LOGIN_TYPE)
public class ShopAddressController {

    private final AddressService addressService;

    @GetMapping
    public Result<List<AddressVO>> list() {
        return Result.ok(addressService.list(ShopLoginContext.currentUserId()));
    }

    @PostMapping
    public Result<AddressVO> create(@Valid @RequestBody AddressSaveRequest request) {
        return Result.ok(addressService.create(ShopLoginContext.currentUserId(), request));
    }

    @PutMapping("/{id}")
    public Result<AddressVO> update(@PathVariable Long id,
            @Valid @RequestBody AddressSaveRequest request) {
        return Result.ok(addressService.update(
                ShopLoginContext.currentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(ShopLoginContext.currentUserId(), id);
        return Result.ok();
    }

    @PutMapping("/{id}/default")
    public Result<AddressVO> setDefault(@PathVariable Long id) {
        return Result.ok(addressService.setDefault(ShopLoginContext.currentUserId(), id));
    }
}
