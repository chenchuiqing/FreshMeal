package com.freshmeal.controller;

import com.freshmeal.common.PageResult;
import com.freshmeal.common.Result;
import com.freshmeal.dto.user.ShopProductQuery;
import com.freshmeal.service.ProductService;
import com.freshmeal.vo.user.ShopProductDetailVO;
import com.freshmeal.vo.user.ShopProductVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 用户端商品接口：浏览、搜索与筛选免登录，加购与下单在阶段三引入登录校验。 */
@RestController
@RequestMapping("/api/shop/product")
@RequiredArgsConstructor
public class ShopProductController {

    private final ProductService productService;

    @GetMapping("/page")
    public Result<PageResult<ShopProductVO>> page(@Valid ShopProductQuery query) {
        return Result.ok(productService.pageShopProducts(query));
    }

    @GetMapping("/{id}")
    public Result<ShopProductDetailVO> detail(@PathVariable Long id) {
        return Result.ok(productService.getShopDetail(id));
    }

    @GetMapping("/hot")
    public Result<List<ShopProductVO>> hot(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return Result.ok(productService.listHotProducts(limit));
    }

    @GetMapping("/recommend")
    public Result<List<ShopProductVO>> recommend(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return Result.ok(productService.listRecommendedProducts(limit));
    }

    @GetMapping("/health-tags")
    public Result<List<String>> healthTags() {
        return Result.ok(productService.listHealthTags());
    }
}
