package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.freshmeal.common.PageResult;
import com.freshmeal.common.Result;
import com.freshmeal.dto.admin.AdminReviewQuery;
import com.freshmeal.dto.admin.ReviewReplyRequest;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.ReviewService;
import com.freshmeal.vo.admin.AdminReviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/review")
@RequiredArgsConstructor
@SaCheckRole(StpInterfaceImpl.ROLE_ADMIN)
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping("/page")
    public Result<PageResult<AdminReviewVO>> page(@Valid AdminReviewQuery query) {
        return Result.ok(reviewService.pageAdmin(query));
    }

    @PutMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id,
            @Valid @RequestBody ReviewReplyRequest request) {
        reviewService.reply(id, request.reply());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return Result.ok();
    }
}
