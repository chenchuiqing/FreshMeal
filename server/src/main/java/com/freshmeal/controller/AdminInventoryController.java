package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.freshmeal.common.PageResult;
import com.freshmeal.common.Result;
import com.freshmeal.dto.admin.AdminBatchLossRequest;
import com.freshmeal.dto.admin.AdminBatchQuery;
import com.freshmeal.dto.admin.AdminBatchSaveRequest;
import com.freshmeal.dto.admin.InventoryLogQuery;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.AdminInventoryService;
import com.freshmeal.vo.admin.AdminBatchVO;
import com.freshmeal.vo.admin.AdminInventoryLogVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SaCheckRole(StpInterfaceImpl.ROLE_ADMIN)
public class AdminInventoryController {

    private final AdminInventoryService inventoryService;

    @GetMapping("/batch/page")
    public Result<PageResult<AdminBatchVO>> batches(@Valid AdminBatchQuery query) {
        return Result.ok(inventoryService.batches(query));
    }

    @GetMapping("/batch/expire-warning")
    public Result<List<AdminBatchVO>> expireWarning(
            @RequestParam(defaultValue = "3") int days) {
        return Result.ok(inventoryService.expireWarning(days));
    }

    @PostMapping("/batch")
    public Result<AdminBatchVO> createBatch(
            @Valid @RequestBody AdminBatchSaveRequest request) {
        return Result.ok(inventoryService.createBatch(
                request, StpUtil.getLoginIdAsLong()));
    }

    @PutMapping("/batch/{id}/loss")
    public Result<AdminBatchVO> reportLoss(
            @PathVariable Long id,
            @Valid @RequestBody AdminBatchLossRequest request) {
        return Result.ok(inventoryService.reportLoss(
                id, request, StpUtil.getLoginIdAsLong()));
    }

    @GetMapping("/inventory/log")
    public Result<PageResult<AdminInventoryLogVO>> logs(
            @Valid InventoryLogQuery query) {
        return Result.ok(inventoryService.logs(query));
    }
}
