package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.freshmeal.common.Result;
import com.freshmeal.dto.user.CartItemAddRequest;
import com.freshmeal.dto.user.CartItemUpdateRequest;
import com.freshmeal.dto.user.OrderSettleRequest;
import com.freshmeal.security.ShopLoginContext;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.CartService;
import com.freshmeal.service.OrderService;
import com.freshmeal.vo.user.CartLineVO;
import com.freshmeal.vo.user.CartSummaryVO;
import com.freshmeal.vo.user.OrderSettleVO;
import jakarta.validation.Valid;
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
@RequestMapping("/api/shop/cart")
@RequiredArgsConstructor
@SaCheckLogin(type = StpInterfaceImpl.USER_LOGIN_TYPE)
public class ShopCartController {

    private final CartService cartService;
    private final OrderService orderService;

    @GetMapping
    public Result<CartSummaryVO> list() {
        return Result.ok(cartService.list(ShopLoginContext.currentUserId()));
    }

    @PostMapping
    public Result<CartLineVO> add(@Valid @RequestBody CartItemAddRequest request) {
        return Result.ok(cartService.add(ShopLoginContext.currentUserId(), request));
    }

    @PutMapping("/{id}")
    public Result<CartLineVO> update(@PathVariable Long id,
            @Valid @RequestBody CartItemUpdateRequest request) {
        return Result.ok(cartService.update(
                ShopLoginContext.currentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        cartService.delete(ShopLoginContext.currentUserId(), id);
        return Result.ok();
    }

    @PostMapping("/settle")
    public Result<OrderSettleVO> settle(@Valid @RequestBody OrderSettleRequest request) {
        return Result.ok(orderService.settle(ShopLoginContext.currentUserId(), request));
    }
}
