package com.freshmeal.controller;

import com.freshmeal.common.Result;
import com.freshmeal.service.ContentService;
import com.freshmeal.vo.user.BannerVO;
import com.freshmeal.vo.user.NoticeVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop/content")
@RequiredArgsConstructor
public class ShopContentController {

    private final ContentService contentService;

    @GetMapping("/banner")
    public Result<List<BannerVO>> banner() {
        return Result.ok(contentService.activeBanners());
    }

    @GetMapping("/notice")
    public Result<List<NoticeVO>> notice() {
        return Result.ok(contentService.activeNotices());
    }
}
