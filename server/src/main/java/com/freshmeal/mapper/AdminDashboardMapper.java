package com.freshmeal.mapper;

import com.freshmeal.vo.admin.DashboardOverviewVO;
import com.freshmeal.vo.admin.DashboardRankVO;
import com.freshmeal.vo.admin.DashboardRegionVO;
import com.freshmeal.vo.admin.DashboardTrendVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminDashboardMapper {

    @Select("""
            SELECT
                COALESCE((
                    SELECT SUM(pay_amount)
                    FROM t_order
                    WHERE pay_status = 1
                      AND status <> 9
                      AND DATE(pay_time) = CURDATE()
                ), 0) AS todayRevenue,
                (
                    SELECT COUNT(*)
                    FROM t_order
                    WHERE create_time >= CURDATE()
                ) AS todayOrderCount,
                (
                    SELECT COUNT(*)
                    FROM t_user
                    WHERE deleted = 0
                      AND create_time >= CURDATE()
                ) AS todayUserCount,
                (
                    SELECT COUNT(*)
                    FROM t_order
                    WHERE status IN (3, 4)
                ) AS pendingDeliveryCount,
                (
                    SELECT COUNT(*)
                    FROM t_order
                    WHERE finish_time >= CURDATE()
                ) AS finishedTodayCount
            """)
    DashboardOverviewVO selectOverview();

    @Select("""
            SELECT
                DATE(create_time) AS statDate,
                COALESCE(SUM(
                    CASE
                        WHEN pay_status = 1 AND status <> 9
                            THEN pay_amount
                        ELSE 0
                    END
                ), 0) AS revenue,
                COUNT(*) AS orderCount
            FROM t_order
            WHERE create_time >= #{startTime}
              AND create_time < DATE_ADD(CURDATE(), INTERVAL 1 DAY)
            GROUP BY DATE(create_time)
            ORDER BY DATE(create_time)
            """)
    List<DashboardTrendVO> selectTrend(@Param("startTime") LocalDateTime startTime);

    @Select("""
            SELECT
                i.product_id AS productId,
                MAX(i.product_name) AS productName,
                SUM(i.quantity) AS quantity,
                SUM(i.subtotal) AS revenue
            FROM t_order_item i
            JOIN t_order o ON o.id = i.order_id
            WHERE o.pay_status = 1
              AND o.status <> 9
            GROUP BY i.product_id
            ORDER BY quantity DESC, revenue DESC
            LIMIT #{limitValue}
            """)
    List<DashboardRankVO> selectRank(@Param("limitValue") int limit);

    @Select("""
            SELECT
                COALESCE(a.id, 0) AS areaId,
                COALESCE(a.name, '未分配区域') AS areaName,
                COUNT(*) AS orderCount,
                COALESCE(SUM(o.pay_amount), 0) AS revenue
            FROM t_order o
            LEFT JOIN t_delivery_area a ON a.id = o.delivery_area_id
            WHERE o.delivery_type = 1
              AND o.pay_status = 1
              AND o.status <> 9
            GROUP BY COALESCE(a.id, 0), COALESCE(a.name, '未分配区域')
            ORDER BY orderCount DESC, revenue DESC
            """)
    List<DashboardRegionVO> selectRegion();
}
