package com.freshmeal.service;

import com.freshmeal.vo.admin.DashboardOverviewVO;
import com.freshmeal.vo.admin.DashboardRankVO;
import com.freshmeal.vo.admin.DashboardRegionVO;
import com.freshmeal.vo.admin.DashboardTrendVO;
import java.util.List;

public interface AdminDashboardService {

    DashboardOverviewVO overview();

    List<DashboardTrendVO> trend(int days);

    List<DashboardRankVO> rank(int limit);

    List<DashboardRegionVO> region();
}
