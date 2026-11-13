package com.freshmeal.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.freshmeal.common.BusinessException;
import com.freshmeal.dto.admin.CategorySaveRequest;
import com.freshmeal.entity.Category;
import com.freshmeal.entity.Product;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.mapper.CategoryMapper;
import com.freshmeal.mapper.ProductMapper;
import com.freshmeal.service.CategoryService;
import com.freshmeal.vo.product.CategoryVO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final Long ROOT_PARENT_ID = 0L;
    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 0;

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    @Override
    public List<CategoryVO> listAdminTree() {
        return buildTree(selectOrdered(null));
    }

    @Override
    public List<CategoryVO> listShopTree() {
        List<Category> enabled = selectOrdered(STATUS_ENABLED);
        Set<Long> enabledIds = enabled.stream().map(Category::getId).collect(Collectors.toSet());
        List<Category> visible = enabled.stream()
                .filter(category -> ROOT_PARENT_ID.equals(category.getParentId())
                        || enabledIds.contains(category.getParentId()))
                .toList();
        return buildTree(visible);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CategorySaveRequest request) {
        Long parentId = normalizeParentId(request.parentId());
        validateParent(parentId, null);
        String name = request.name().trim();
        ensureNameAvailable(parentId, name, null);

        Category category = new Category();
        category.setParentId(parentId);
        category.setName(name);
        category.setIcon(normalizeText(request.icon()));
        category.setSort(request.sort() == null ? 0 : request.sort());
        category.setStatus(request.status() == null ? STATUS_ENABLED : request.status());
        category.setDeleted(0);
        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CategorySaveRequest request) {
        Category existing = requireCategory(id);
        Long parentId = normalizeParentId(request.parentId());
        if (id.equals(parentId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "分类不能作为自己的父分类");
        }
        validateParent(parentId, id);
        if (!ROOT_PARENT_ID.equals(parentId) && hasChildren(id)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "存在子分类的一级分类不能移动到其他分类下");
        }
        String name = request.name().trim();
        ensureNameAvailable(parentId, name, id);

        int status = request.status() == null ? existing.getStatus() : request.status();
        if (status == STATUS_DISABLED && hasChildren(id)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "请先停用该分类下的子分类");
        }

        categoryMapper.update(null, Wrappers.<Category>lambdaUpdate()
                .eq(Category::getId, id)
                .set(Category::getParentId, parentId)
                .set(Category::getName, name)
                .set(Category::getIcon, normalizeText(request.icon()))
                .set(Category::getSort, request.sort() == null ? existing.getSort() : request.sort())
                .set(Category::getStatus, status));
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        requireCategory(id);
        if (status == STATUS_DISABLED && hasChildren(id)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "请先停用该分类下的子分类");
        }
        categoryMapper.update(null, Wrappers.<Category>lambdaUpdate()
                .eq(Category::getId, id)
                .set(Category::getStatus, status));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireCategory(id);
        if (hasChildren(id)) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_CHILDREN.getCode(),
                    ErrorCode.CATEGORY_HAS_CHILDREN.getMessage());
        }
        Long productCount = productMapper.selectCount(Wrappers.<Product>lambdaQuery()
                .eq(Product::getCategoryId, id));
        if (productCount != null && productCount > 0) {
            throw new BusinessException(ErrorCode.CATEGORY_HAS_PRODUCTS.getCode(),
                    ErrorCode.CATEGORY_HAS_PRODUCTS.getMessage());
        }
        categoryMapper.deleteById(id);
    }

    private List<Category> selectOrdered(Integer status) {
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .eq(status != null, Category::getStatus, status)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
    }

    private List<CategoryVO> buildTree(List<Category> categories) {
        Map<Long, List<Category>> childrenByParent = categories.stream()
                .collect(Collectors.groupingBy(Category::getParentId, LinkedHashMap::new,
                        Collectors.toList()));
        return buildChildren(ROOT_PARENT_ID, childrenByParent);
    }

    private List<CategoryVO> buildChildren(Long parentId, Map<Long, List<Category>> childrenByParent) {
        return childrenByParent.getOrDefault(parentId, List.of()).stream()
                .map(category -> new CategoryVO(
                        category.getId(),
                        category.getParentId(),
                        category.getName(),
                        category.getIcon(),
                        category.getSort(),
                        category.getStatus(),
                        buildChildren(category.getId(), childrenByParent)))
                .toList();
    }

    private Category requireCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND.getCode(),
                    ErrorCode.CATEGORY_NOT_FOUND.getMessage());
        }
        return category;
    }

    private void validateParent(Long parentId, Long currentId) {
        if (ROOT_PARENT_ID.equals(parentId)) {
            return;
        }
        Category parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND.getCode(), "父分类不存在");
        }
        if (!ROOT_PARENT_ID.equals(parent.getParentId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "分类最多支持两级");
        }
        if (parentId.equals(currentId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "分类不能作为自己的父分类");
        }
    }

    private void ensureNameAvailable(Long parentId, String name, Long excludeId) {
        boolean exists = categoryMapper.exists(Wrappers.<Category>lambdaQuery()
                .eq(Category::getParentId, parentId)
                .eq(Category::getName, name)
                .ne(excludeId != null, Category::getId, excludeId));
        if (exists) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "同级分类名称已存在");
        }
    }

    private boolean hasChildren(Long id) {
        return categoryMapper.exists(Wrappers.<Category>lambdaQuery()
                .eq(Category::getParentId, id));
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null ? ROOT_PARENT_ID : parentId;
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
