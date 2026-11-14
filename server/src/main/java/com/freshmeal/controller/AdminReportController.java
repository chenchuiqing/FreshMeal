package com.freshmeal.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.alibaba.excel.EasyExcel;
import com.freshmeal.security.StpInterfaceImpl;
import com.freshmeal.service.AdminReportService;
import com.freshmeal.vo.admin.OrderReportRow;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/report")
@RequiredArgsConstructor
@SaCheckRole(StpInterfaceImpl.ROLE_ADMIN)
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping("/orders.xlsx")
    public void exportOrders(HttpServletResponse response,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime)
            throws IOException {
        List<OrderReportRow> rows = adminReportService.orderRows(startTime, endTime);
        String fileName = URLEncoder.encode("FreshMeal-订单报表.xlsx",
                StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + fileName);
        EasyExcel.write(response.getOutputStream(), OrderReportRow.class)
                .sheet("订单明细")
                .doWrite(rows);
    }
}
