package com.freshmeal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freshmeal.dto.user.ShopProductQuery;
import com.freshmeal.entity.Product;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    IPage<Product> selectShopPage(
            Page<Product> page,
            @Param("query") ShopProductQuery query,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("healthTags") List<String> healthTags,
            @Param("keywordLike") boolean keywordLike);

    List<String> selectShopHealthTags();
}
