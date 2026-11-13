package com.freshmeal.service;

import com.freshmeal.dto.admin.CategorySaveRequest;
import com.freshmeal.vo.product.CategoryVO;
import java.util.List;

public interface CategoryService {

    /** 后台分类树，包含停用分类。 */
    List<CategoryVO> listAdminTree();

    /** 用户端分类树，仅包含启用分类。 */
    List<CategoryVO> listShopTree();

    Long create(CategorySaveRequest request);

    void update(Long id, CategorySaveRequest request);

    void updateStatus(Long id, Integer status);

    void delete(Long id);
}
