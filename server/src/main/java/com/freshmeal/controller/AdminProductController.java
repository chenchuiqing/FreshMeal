package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.freshmeal.common.PageResult;
import com.freshmeal.common.Result;
import com.freshmeal.dto.admin.AdminProductQuery;
import com.freshmeal.dto.admin.ProductSaveRequest;
import com.freshmeal.dto.admin.RecommendRequest;
import com.freshmeal.dto.admin.StatusRequest;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.ProductService;
import com.freshmeal.vo.admin.AdminProductDetailVO;
import com.freshmeal.vo.admin.AdminProductVO;
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

/** 管理端商品维护接口，全部要求管理员角色。 */
@RestController
@RequestMapping("/api/admin/product")
@RequiredArgsConstructor
@SaCheckRole(StpInterfaceImpl.ROLE_ADMIN)
public class AdminProductController {

    private final ProductService productService;

    @GetMapping("/page")
    public Result<PageResult<AdminProductVO>> page(@Valid AdminProductQuery query) {
        return Result.ok(productService.pageAdminProducts(query));
    }

    @GetMapping("/{id}")
    public Result<AdminProductDetailVO> detail(@PathVariable Long id) {
        return Result.ok(productService.getAdminDetail(id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody ProductSaveRequest request) {
        return Result.ok(productService.create(request));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductSaveRequest request) {
        productService.update(id, request);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        productService.updateStatus(id, request.status());
        return Result.ok();
    }

    @PutMapping("/{id}/recommend")
    public Result<Void> updateRecommend(@PathVariable Long id, @Valid @RequestBody RecommendRequest request) {
        productService.updateRecommend(id, request.isRecommend());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.ok();
    }
}
