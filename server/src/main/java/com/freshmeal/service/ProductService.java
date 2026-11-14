package com.freshmeal.service;

import com.freshmeal.common.PageResult;
import com.freshmeal.dto.admin.AdminProductQuery;
import com.freshmeal.dto.admin.ProductSaveRequest;
import com.freshmeal.dto.user.ShopProductQuery;
import com.freshmeal.vo.admin.AdminProductDetailVO;
import com.freshmeal.vo.admin.AdminProductVO;
import com.freshmeal.vo.user.ShopProductDetailVO;
import com.freshmeal.vo.user.ShopProductVO;
import java.util.List;

public interface ProductService {

    PageResult<AdminProductVO> pageAdminProducts(AdminProductQuery query);

    AdminProductDetailVO getAdminDetail(Long id);

    Long create(ProductSaveRequest request);

    void update(Long id, ProductSaveRequest request);

    void updateStatus(Long id, Integer status);

    void updateRecommend(Long id, Integer isRecommend);

    void delete(Long id);

    PageResult<ShopProductVO> pageShopProducts(ShopProductQuery query);

    ShopProductDetailVO getShopDetail(Long id);

    List<ShopProductVO> listHotProducts(int limit);

    List<ShopProductVO> listRecommendedProducts(int limit);

    List<String> listHealthTags();

    List<ShopProductVO> listShopProductsByIds(List<Long> ids);
}
