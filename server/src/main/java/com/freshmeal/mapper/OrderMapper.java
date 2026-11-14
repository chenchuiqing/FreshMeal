package com.freshmeal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.freshmeal.entity.Order;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("""
            SELECT * FROM t_order WHERE order_no = #{orderNo} LIMIT 1 FOR UPDATE
            """)
    Order selectByOrderNoForUpdate(@Param("orderNo") String orderNo);

    @Select("""
            SELECT * FROM t_order
            WHERE status = 0 AND create_time < #{cutoff}
            ORDER BY create_time
            LIMIT 100
            """)
    List<Order> selectUnpaidTimeouts(@Param("cutoff") LocalDateTime cutoff);

    @Update("""
            UPDATE t_order
            SET status = #{targetStatus},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND status = #{fromStatus}
            """)
    int transitionStatus(
            @Param("id") Long id,
            @Param("fromStatus") int fromStatus,
            @Param("targetStatus") int targetStatus);
}
