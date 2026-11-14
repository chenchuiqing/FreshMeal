package com.freshmeal.service;

import com.freshmeal.vo.admin.OrderReportRow;
import java.util.List;

public interface AdminReportService {

    List<OrderReportRow> orderRows(String startTime, String endTime);
}
