package com.freshmeal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.freshmeal.entity.DeliveryOrder;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DeliveryOrderMapper extends BaseMapper<DeliveryOrder> {

    @Select("""
            SELECT * FROM t_delivery_order
            WHERE delivery_no = #{deliveryNo} LIMIT 1 FOR UPDATE
            """)
    DeliveryOrder selectByDeliveryNoForUpdate(@Param("deliveryNo") String deliveryNo);

    @Select("""
            SELECT * FROM t_delivery_order
            WHERE order_id = #{orderId} LIMIT 1 FOR UPDATE
            """)
    DeliveryOrder selectByOrderIdForUpdate(@Param("orderId") Long orderId);

    @Update("""
            UPDATE t_delivery_order
            SET delivery_man_id = #{riderId},
                delivery_man_name = #{riderName},
                delivery_man_phone = #{riderPhone},
                status = 1,
                assign_type = #{assignType},
                assign_time = CURRENT_TIMESTAMP,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND status = 0
            """)
    int claimForRider(
            @Param("id") Long id,
            @Param("riderId") Long riderId,
            @Param("riderName") String riderName,
            @Param("riderPhone") String riderPhone,
            @Param("assignType") int assignType);

    @Update("""
            UPDATE t_delivery_order
            SET status = 0,
                delivery_man_id = NULL,
                delivery_man_name = '',
                delivery_man_phone = '',
                assign_type = 0,
                assign_time = NULL,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND status = 1
            """)
    int resetClaim(@Param("id") Long id);

    @Update("""
            UPDATE t_delivery_order
            SET status = #{targetStatus},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND status = #{fromStatus}
            """)
    int transitionStatus(
            @Param("id") Long id,
            @Param("fromStatus") int fromStatus,
            @Param("targetStatus") int targetStatus);

    @Update("""
            UPDATE t_delivery_order
            SET pickup_time = CURRENT_TIMESTAMP,
                pickup_address = #{pickupAddress},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND status = 1
            """)
    int markPickedUp(@Param("id") Long id, @Param("pickupAddress") String pickupAddress);

    @Update("""
            UPDATE t_delivery_order
            SET receive_time = CURRENT_TIMESTAMP,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND status = 3
            """)
    int markReceived(@Param("id") Long id);

    @Select("""
            SELECT * FROM t_delivery_order
            WHERE status = 0
            ORDER BY create_time, id
            LIMIT #{limit}
            """)
    List<DeliveryOrder> selectWaiting(@Param("limit") int limit);
}
