package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.freshmeal.common.PageResult;
import com.freshmeal.common.Result;
import com.freshmeal.dto.admin.BannerSaveRequest;
import com.freshmeal.dto.admin.NoticeSaveRequest;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.ContentService;
import com.freshmeal.vo.admin.AdminBannerVO;
import com.freshmeal.vo.admin.AdminNoticeVO;
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
@RequestMapping("/api/admin/content")
@RequiredArgsConstructor
@SaCheckRole(StpInterfaceImpl.ROLE_ADMIN)
public class AdminContentController {

    private final ContentService contentService;

    @GetMapping("/banner")
    public Result<PageResult<AdminBannerVO>> banners() {
        return Result.ok(contentService.pageBanners());
    }

    @PostMapping("/banner")
    public Result<Long> createBanner(@Valid @RequestBody BannerSaveRequest request) {
        return Result.ok(contentService.createBanner(request));
    }

    @PutMapping("/banner/{id}")
    public Result<Void> updateBanner(@PathVariable Long id,
            @Valid @RequestBody BannerSaveRequest request) {
        contentService.updateBanner(id, request);
        return Result.ok();
    }

    @DeleteMapping("/banner/{id}")
    public Result<Void> deleteBanner(@PathVariable Long id) {
        contentService.deleteBanner(id);
        return Result.ok();
    }

    @GetMapping("/notice")
    public Result<PageResult<AdminNoticeVO>> notices() {
        return Result.ok(contentService.pageNotices());
    }

    @PostMapping("/notice")
    public Result<Long> createNotice(@Valid @RequestBody NoticeSaveRequest request) {
        return Result.ok(contentService.createNotice(request));
    }

    @PutMapping("/notice/{id}")
    public Result<Void> updateNotice(@PathVariable Long id,
            @Valid @RequestBody NoticeSaveRequest request) {
        contentService.updateNotice(id, request);
        return Result.ok();
    }

    @DeleteMapping("/notice/{id}")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        contentService.deleteNotice(id);
        return Result.ok();
    }
}
