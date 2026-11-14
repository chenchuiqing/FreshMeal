package com.freshmeal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.freshmeal.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

    @Select("""
            SELECT * FROM t_payment WHERE pay_no = #{payNo} LIMIT 1 FOR UPDATE
            """)
    Payment selectByPayNoForUpdate(@Param("payNo") String payNo);

    @Update("""
            UPDATE t_payment
            SET status = 3, update_time = CURRENT_TIMESTAMP
            WHERE order_id = #{orderId} AND status = 0
            """)
    int closePendingByOrder(@Param("orderId") Long orderId);
}
