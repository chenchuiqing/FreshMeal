package com.freshmeal.vo.admin;

import com.alibaba.excel.annotation.ExcelProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderReportRow(
        @ExcelProperty("订单号")
        String orderNo,

        @ExcelProperty("用户名")
        String username,

        @ExcelProperty("订单状态")
        String status,

        @ExcelProperty("下单时间")
        LocalDateTime createTime,

        @ExcelProperty("商品名称")
        String productName,

        @ExcelProperty("规格")
        String specName,

        @ExcelProperty("数量")
        Integer quantity,

        @ExcelProperty("商品小计")
        BigDecimal subtotal,

        @ExcelProperty("订单实付")
        BigDecimal payAmount
) {
}
