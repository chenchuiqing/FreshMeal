package com.freshmeal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freshmeal.dto.user.ShopProductQuery;
import com.freshmeal.entity.Product;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    IPage<Product> selectShopPage(
            Page<Product> page,
            @Param("query") ShopProductQuery query,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("healthTags") List<String> healthTags,
            @Param("keywordLike") boolean keywordLike);

    List<String> selectShopHealthTags();

    @Update("""
            UPDATE t_product
            SET stock = stock - #{quantity},
                version = version + 1,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND deleted = 0
              AND status = 1
              AND stock >= #{quantity}
            """)
    int deductStockOptimistically(
            @Param("id") Long id,
            @Param("quantity") int quantity);

    @Update("""
            UPDATE t_product
            SET stock = stock + #{quantity},
                version = version + 1,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND deleted = 0
            """)
    int restoreStock(
            @Param("id") Long id,
            @Param("quantity") int quantity);

    @Update("""
            UPDATE t_product
            SET stock = stock - #{quantity},
                version = version + 1,
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND deleted = 0
              AND stock >= #{quantity}
            """)
    int deductStockForInventory(
            @Param("id") Long id,
            @Param("quantity") int quantity);

    @Update("""
            UPDATE t_product
            SET sales_count = sales_count + #{quantity},
                update_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND deleted = 0
            """)
    int increaseSalesCount(
            @Param("id") Long id,
            @Param("quantity") int quantity);
}
