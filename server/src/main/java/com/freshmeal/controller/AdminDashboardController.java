package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.freshmeal.common.Result;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.AdminDashboardService;
import com.freshmeal.vo.admin.DashboardOverviewVO;
import com.freshmeal.vo.admin.DashboardRankVO;
import com.freshmeal.vo.admin.DashboardRegionVO;
import com.freshmeal.vo.admin.DashboardTrendVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@SaCheckRole(StpInterfaceImpl.ROLE_ADMIN)
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/overview")
    public Result<DashboardOverviewVO> overview() {
        return Result.ok(dashboardService.overview());
    }

    @GetMapping("/trend")
    public Result<List<DashboardTrendVO>> trend(
            @RequestParam(defaultValue = "7") int days) {
        return Result.ok(dashboardService.trend(days));
    }

    @GetMapping("/rank")
    public Result<List<DashboardRankVO>> rank(
            @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(dashboardService.rank(limit));
    }

    @GetMapping("/region")
    public Result<List<DashboardRegionVO>> region() {
        return Result.ok(dashboardService.region());
    }
}
