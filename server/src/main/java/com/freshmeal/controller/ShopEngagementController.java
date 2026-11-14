package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.freshmeal.common.PageResult;
import com.freshmeal.common.Result;
import com.freshmeal.dto.common.PageQuery;
import com.freshmeal.security.ShopLoginContext;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.EngagementService;
import com.freshmeal.vo.user.ShopProductVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
@SaCheckLogin(type = StpInterfaceImpl.USER_LOGIN_TYPE)
public class ShopEngagementController {

    private final EngagementService engagementService;

    @GetMapping("/favorite/ids")
    public Result<List<Long>> favoriteIds() {
        return Result.ok(engagementService.favoriteIds(
                ShopLoginContext.currentUserId()));
    }

    @GetMapping("/favorite/page")
    public Result<PageResult<ShopProductVO>> favoritePage(@Valid PageQuery query) {
        return Result.ok(engagementService.favoritePage(
                ShopLoginContext.currentUserId(), query.getCurrent(), query.getSize()));
    }

    @PostMapping("/favorite/{productId}")
    public Result<Boolean> toggleFavorite(@PathVariable Long productId) {
        return Result.ok(engagementService.toggleFavorite(
                ShopLoginContext.currentUserId(), productId));
    }

    @PostMapping("/history/{productId}")
    public Result<Void> recordHistory(@PathVariable Long productId) {
        engagementService.recordHistory(ShopLoginContext.currentUserId(), productId);
        return Result.ok();
    }

    @GetMapping("/history/page")
    public Result<PageResult<ShopProductVO>> historyPage(@Valid PageQuery query) {
        return Result.ok(engagementService.historyPage(
                ShopLoginContext.currentUserId(), query.getCurrent(), query.getSize()));
    }
}
