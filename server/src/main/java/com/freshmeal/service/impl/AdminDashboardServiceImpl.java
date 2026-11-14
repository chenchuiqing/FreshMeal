package com.freshmeal.service.impl;

import com.freshmeal.mapper.AdminDashboardMapper;
import com.freshmeal.service.AdminDashboardService;
import com.freshmeal.vo.admin.DashboardOverviewVO;
import com.freshmeal.vo.admin.DashboardRankVO;
import com.freshmeal.vo.admin.DashboardRegionVO;
import com.freshmeal.vo.admin.DashboardTrendVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final AdminDashboardMapper adminDashboardMapper;

    @Override
    public DashboardOverviewVO overview() {
        return adminDashboardMapper.selectOverview();
    }

    @Override
    public List<DashboardTrendVO> trend(int days) {
        int safeDays = Math.max(7, Math.min(days, 90));
        LocalDateTime startTime = LocalDate.now()
                .minusDays(safeDays - 1L)
                .atStartOfDay();
        return adminDashboardMapper.selectTrend(startTime);
    }

    @Override
    public List<DashboardRankVO> rank(int limit) {
        return adminDashboardMapper.selectRank(Math.max(1, Math.min(limit, 50)));
    }

    @Override
    public List<DashboardRegionVO> region() {
        return adminDashboardMapper.selectRegion();
    }
}
