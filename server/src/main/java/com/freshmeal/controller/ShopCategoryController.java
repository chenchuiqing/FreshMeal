package com.freshmeal.controller;

import com.freshmeal.common.Result;
import com.freshmeal.service.CategoryService;
import com.freshmeal.vo.product.CategoryVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 用户端分类接口：浏览类接口免登录。 */
@RestController
@RequestMapping("/api/shop/category")
@RequiredArgsConstructor
public class ShopCategoryController {

    private final CategoryService categoryService;

    @GetMapping("/tree")
    public Result<List<CategoryVO>> tree() {
        return Result.ok(categoryService.listShopTree());
    }
}
