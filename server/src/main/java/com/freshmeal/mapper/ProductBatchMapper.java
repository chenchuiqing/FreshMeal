package com.freshmeal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.freshmeal.entity.ProductBatch;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductBatchMapper extends BaseMapper<ProductBatch> {

    @Select("""
            SELECT * FROM t_product_batch
            WHERE product_id = #{productId}
              AND status <> 4
              AND status <> 3
              AND stock > 0
              AND expire_date >= CURRENT_DATE
            ORDER BY expire_date ASC, id ASC
            """)
    List<ProductBatch> selectAvailableFefo(@Param("productId") Long productId);

    @Update("""
            UPDATE t_product_batch
            SET stock = stock - #{quantity},
                status = CASE
                    WHEN stock - #{quantity} <= 0 THEN 4
                    WHEN expire_date < CURRENT_DATE THEN 3
                    WHEN expire_date <= CURRENT_DATE + INTERVAL 3 DAY THEN 2
                    ELSE 1
                END,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND stock >= #{quantity}
            """)
    int deductStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Update("""
            UPDATE t_product_batch
            SET stock = stock + #{quantity},
                status = CASE
                    WHEN expire_date < CURRENT_DATE THEN 3
                    WHEN expire_date <= CURRENT_DATE + INTERVAL 3 DAY THEN 2
                    ELSE 1
                END,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int restoreStock(@Param("id") Long id, @Param("quantity") int quantity);
}
