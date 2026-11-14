package com.freshmeal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freshmeal.common.BusinessException;
import com.freshmeal.common.PageResult;
import com.freshmeal.dto.admin.AdminProductQuery;
import com.freshmeal.dto.admin.ProductImageRequest;
import com.freshmeal.dto.admin.ProductOptionItemRequest;
import com.freshmeal.dto.admin.ProductOptionRequest;
import com.freshmeal.dto.admin.ProductSaveRequest;
import com.freshmeal.dto.admin.ProductSpecRequest;
import com.freshmeal.dto.user.ShopProductQuery;
import com.freshmeal.entity.Category;
import com.freshmeal.entity.Product;
import com.freshmeal.entity.ProductImage;
import com.freshmeal.entity.ProductOption;
import com.freshmeal.entity.ProductOptionItem;
import com.freshmeal.entity.ProductSpec;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.mapper.CategoryMapper;
import com.freshmeal.mapper.ProductImageMapper;
import com.freshmeal.mapper.ProductMapper;
import com.freshmeal.mapper.ProductOptionItemMapper;
import com.freshmeal.mapper.ProductOptionMapper;
import com.freshmeal.mapper.ProductSpecMapper;
import com.freshmeal.service.ProductService;
import com.freshmeal.vo.admin.AdminProductDetailVO;
import com.freshmeal.vo.admin.AdminProductVO;
import com.freshmeal.vo.product.ProductImageVO;
import com.freshmeal.vo.product.ProductOptionItemVO;
import com.freshmeal.vo.product.ProductOptionVO;
import com.freshmeal.vo.product.ProductSpecVO;
import com.freshmeal.vo.user.ShopProductDetailVO;
import com.freshmeal.vo.user.ShopProductVO;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final int STATUS_ON = 1;
    private static final int MAX_LIST_LIMIT = 50;
    /** 纯中文且至少两个字的关键词走 ngram 全文索引（分词按 bigram 滑窗），其余回退 LIKE。 */
    private static final Pattern FULLTEXT_SAFE = Pattern.compile("^[\\u4e00-\\u9fff]{2,}$");

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final ProductSpecMapper specMapper;
    private final ProductOptionMapper optionMapper;
    private final ProductOptionItemMapper optionItemMapper;
    private final ProductImageMapper imageMapper;

    @Override
    public PageResult<AdminProductVO> pageAdminProducts(AdminProductQuery query) {
        List<Long> categoryIds = null;
        if (query.getCategoryId() != null) {
            categoryIds = subtreeIds(query.getCategoryId(), categoryMapper.selectList(null));
        }
        String name = StringUtils.hasText(query.getName()) ? query.getName().trim() : null;
        Page<Product> page = productMapper.selectPage(new Page<>(query.getCurrent(), query.getSize()),
                Wrappers.<Product>lambdaQuery()
                        .like(name != null, Product::getName, name)
                        .in(categoryIds != null, Product::getCategoryId, categoryIds)
                        .eq(query.getStatus() != null, Product::getStatus, query.getStatus())
                        .eq(query.getProductType() != null, Product::getProductType, query.getProductType())
                        .orderByAsc(Product::getSort)
                        .orderByAsc(Product::getId));
        Map<Long, String> names = categoryNameMap(page.getRecords());
        List<AdminProductVO> records = page.getRecords().stream()
                .map(product -> toAdminVo(product, names.get(product.getCategoryId())))
                .toList();
        return PageResult.of(records, page.getTotal(), query.getCurrent(), query.getSize());
    }

    @Override
    public AdminProductDetailVO getAdminDetail(Long id) {
        Product product = requireProduct(id);
        List<ProductSpec> specs = selectSpecs(id, null);
        List<ProductOptionVO> options = selectOptionVos(id, null);
        List<ProductImageVO> images = selectImageVos(id, null);
        return new AdminProductDetailVO(
                product.getId(),
                product.getCategoryId(),
                categoryName(product.getCategoryId()),
                product.getName(),
                product.getSubtitle(),
                product.getCover(),
                product.getDetail(),
                product.getProductType(),
                product.getPrice(),
                product.getOriginalPrice(),
                product.getUnit(),
                product.getStock(),
                product.getSalesCount(),
                product.getStatus(),
                product.getIsRecommend(),
                product.getSort(),
                product.getCalories(),
                product.getProtein(),
                product.getFat(),
                product.getCarbohydrate(),
                product.getSodium(),
                product.getNutritionDesc(),
                splitCsv(product.getAllergens()),
                product.getStorageType(),
                product.getShelfLifeDays(),
                product.getProducer(),
                product.getHeatingMethod(),
                splitCsv(product.getHealthTags()),
                toSpecVos(specs),
                options,
                images);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProductSaveRequest request) {
        requireCategory(request.categoryId());
        String allergens = joinCsv(request.allergens(), 255, "过敏原");
        String healthTags = joinCsv(request.healthTags(), 100, "健康标签");

        Product product = new Product();
        product.setCategoryId(request.categoryId());
        product.setName(request.name().trim());
        product.setSubtitle(normalizeText(request.subtitle()));
        product.setCover(request.cover().trim());
        product.setDetail(normalizeText(request.detail()));
        product.setProductType(request.productType() == null ? 1 : request.productType());
        product.setPrice(request.price());
        product.setOriginalPrice(request.originalPrice());
        product.setUnit(request.unit().trim());
        product.setStock(request.stock());
        product.setStatus(request.status() == null ? STATUS_ON : request.status());
        product.setIsRecommend(request.isRecommend() == null ? 0 : request.isRecommend());
        product.setSort(request.sort() == null ? 0 : request.sort());
        product.setSalesCount(0);
        product.setVersion(0);
        product.setCalories(request.calories());
        product.setProtein(request.protein());
        product.setFat(request.fat());
        product.setCarbohydrate(request.carbohydrate());
        product.setSodium(request.sodium());
        product.setNutritionDesc(normalizeText(request.nutritionDesc()));
        product.setAllergens(allergens);
        product.setStorageType(request.storageType());
        product.setShelfLifeDays(request.shelfLifeDays());
        product.setProducer(normalizeText(request.producer()));
        product.setHeatingMethod(normalizeText(request.heatingMethod()));
        product.setHealthTags(healthTags);
        product.setDeleted(0);
        productMapper.insert(product);

        Long defaultSpecId = saveChildren(product.getId(), request);
        if (defaultSpecId != null) {
            productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                    .eq(Product::getId, product.getId())
                    .set(Product::getDefaultSpecId, defaultSpecId));
        }
        return product.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProductSaveRequest request) {
        Product existing = requireProduct(id);
        requireCategory(request.categoryId());
        String allergens = joinCsv(request.allergens(), 255, "过敏原");
        String healthTags = joinCsv(request.healthTags(), 100, "健康标签");

        deleteChildren(id);
        Long defaultSpecId = saveChildren(id, request);

        productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                .eq(Product::getId, id)
                .set(Product::getCategoryId, request.categoryId())
                .set(Product::getName, request.name().trim())
                .set(Product::getSubtitle, normalizeText(request.subtitle()))
                .set(Product::getCover, request.cover().trim())
                .set(Product::getDetail, normalizeText(request.detail()))
                .set(Product::getProductType, request.productType() == null ? existing.getProductType() : request.productType())
                .set(Product::getPrice, request.price())
                .set(Product::getOriginalPrice, request.originalPrice())
                .set(Product::getUnit, request.unit().trim())
                .set(Product::getStock, request.stock())
                .set(Product::getStatus, request.status() == null ? existing.getStatus() : request.status())
                .set(Product::getIsRecommend, request.isRecommend() == null ? existing.getIsRecommend() : request.isRecommend())
                .set(Product::getSort, request.sort() == null ? existing.getSort() : request.sort())
                .set(Product::getCalories, request.calories())
                .set(Product::getProtein, request.protein())
                .set(Product::getFat, request.fat())
                .set(Product::getCarbohydrate, request.carbohydrate())
                .set(Product::getSodium, request.sodium())
                .set(Product::getNutritionDesc, normalizeText(request.nutritionDesc()))
                .set(Product::getAllergens, allergens)
                .set(Product::getStorageType, request.storageType())
                .set(Product::getShelfLifeDays, request.shelfLifeDays())
                .set(Product::getProducer, normalizeText(request.producer()))
                .set(Product::getHeatingMethod, normalizeText(request.heatingMethod()))
                .set(Product::getHealthTags, healthTags)
                .set(Product::getDefaultSpecId, defaultSpecId));
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        requireProduct(id);
        productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                .eq(Product::getId, id)
                .set(Product::getStatus, status));
    }

    @Override
    public void updateRecommend(Long id, Integer isRecommend) {
        requireProduct(id);
        productMapper.update(null, Wrappers.<Product>lambdaUpdate()
                .eq(Product::getId, id)
                .set(Product::getIsRecommend, isRecommend));
    }

    @Override
    public void delete(Long id) {
        requireProduct(id);
        productMapper.deleteById(id);
    }

    @Override
    public PageResult<ShopProductVO> pageShopProducts(ShopProductQuery query) {
        List<Category> allCategories = categoryMapper.selectList(null);
        Set<Long> enabledIds = allCategories.stream()
                .filter(category -> category.getStatus() == STATUS_ON)
                .map(Category::getId)
                .collect(Collectors.toSet());
        List<Long> categoryIds;
        if (query.getCategoryId() == null) {
            categoryIds = new ArrayList<>(enabledIds);
        } else if (!enabledIds.contains(query.getCategoryId())) {
            return PageResult.of(List.of(), 0, query.getCurrent(), query.getSize());
        } else {
            categoryIds = subtreeIds(query.getCategoryId(), allCategories).stream()
                    .filter(enabledIds::contains)
                    .toList();
        }
        if (categoryIds.isEmpty()) {
            return PageResult.of(List.of(), 0, query.getCurrent(), query.getSize());
        }

        String keyword = StringUtils.hasText(query.getKeyword()) ? query.getKeyword().trim() : null;
        query.setKeyword(keyword);
        boolean keywordLike = keyword != null && !FULLTEXT_SAFE.matcher(keyword).matches();
        List<String> healthTags = splitCsv(query.getHealthTags());

        IPage<Product> page = productMapper.selectShopPage(new Page<>(query.getCurrent(), query.getSize()),
                query, categoryIds, healthTags, keywordLike);
        Map<Long, String> names = categoryNameMap(page.getRecords());
        List<ShopProductVO> records = page.getRecords().stream()
                .map(product -> toShopVo(product, names.get(product.getCategoryId())))
                .toList();
        return PageResult.of(records, page.getTotal(), query.getCurrent(), query.getSize());
    }

    @Override
    public ShopProductDetailVO getShopDetail(Long id) {
        Product product = requireProduct(id);
        Category category = categoryMapper.selectById(product.getCategoryId());
        boolean categoryEnabled = category != null && category.getStatus() == STATUS_ON;
        boolean available = product.getStatus() == STATUS_ON
                && product.getStock() != null && product.getStock() > 0
                && categoryEnabled;
        return new ShopProductDetailVO(
                product.getId(),
                product.getCategoryId(),
                category != null ? category.getName() : null,
                product.getName(),
                product.getSubtitle(),
                product.getCover(),
                product.getDetail(),
                product.getProductType(),
                product.getPrice(),
                product.getOriginalPrice(),
                product.getUnit(),
                product.getStock(),
                product.getSalesCount(),
                product.getStatus(),
                product.getCalories(),
                product.getProtein(),
                product.getFat(),
                product.getCarbohydrate(),
                product.getSodium(),
                product.getNutritionDesc(),
                splitCsv(product.getAllergens()),
                product.getStorageType(),
                storageLabel(product.getStorageType()),
                product.getShelfLifeDays(),
                product.getProducer(),
                product.getHeatingMethod(),
                splitCsv(product.getHealthTags()),
                available,
                toSpecVos(selectSpecs(id, STATUS_ON)),
                selectOptionVos(id, STATUS_ON),
                selectImageVos(id, STATUS_ON));
    }

    @Override
    public List<ShopProductVO> listHotProducts(int limit) {
        return listVisibleProducts(limit, Wrappers.<Product>lambdaQuery()
                .eq(Product::getStatus, STATUS_ON)
                .orderByDesc(Product::getSalesCount)
                .orderByDesc(Product::getId));
    }

    @Override
    public List<ShopProductVO> listRecommendedProducts(int limit) {
        return listVisibleProducts(limit, Wrappers.<Product>lambdaQuery()
                .eq(Product::getStatus, STATUS_ON)
                .eq(Product::getIsRecommend, 1)
                .orderByAsc(Product::getSort)
                .orderByAsc(Product::getId));
    }

    @Override
    public List<String> listHealthTags() {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        for (String row : productMapper.selectShopHealthTags()) {
            tags.addAll(splitCsv(row));
        }
        return List.copyOf(tags);
    }

    @Override
    public List<ShopProductVO> listShopProductsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Product> products = productMapper.selectBatchIds(ids);
        Map<Long, String> names = categoryNameMap(products);
        return products.stream()
                .filter(product -> product.getStatus() != null && product.getStatus() == 1)
                .map(product -> toShopVo(product, names.get(product.getCategoryId())))
                .toList();
    }

    private List<ShopProductVO> listVisibleProducts(int limit, LambdaQueryWrapper<Product> baseQuery) {
        Set<Long> enabledIds = enabledCategoryIds();
        if (enabledIds.isEmpty()) {
            return List.of();
        }
        Page<Product> page = productMapper.selectPage(new Page<>(1, clampLimit(limit)),
                baseQuery.in(Product::getCategoryId, enabledIds));
        Map<Long, String> names = categoryNameMap(page.getRecords());
        return page.getRecords().stream()
                .map(product -> toShopVo(product, names.get(product.getCategoryId())))
                .toList();
    }

    // ---------- 子资源：规格 / 选项 / 图集 ----------

    /**
     * 全量保存商品子资源：删除既有子资源后按排序重建。默认规格取第一个标记为默认的规格，
     * 没有标记时取排序最靠前的规格，返回其 ID 供回写 default_spec_id。
     */
    private Long saveChildren(Long productId, ProductSaveRequest request) {
        List<ProductSpecRequest> specs = sorted(request.specs(), ProductSpecRequest::sort);
        int defaultIndex = 0;
        for (int i = 0; i < specs.size(); i++) {
            if (specs.get(i).isDefault() != null && specs.get(i).isDefault() == 1) {
                defaultIndex = i;
                break;
            }
        }
        Long defaultSpecId = null;
        for (int i = 0; i < specs.size(); i++) {
            ProductSpecRequest specRequest = specs.get(i);
            ProductSpec spec = new ProductSpec();
            spec.setProductId(productId);
            spec.setName(specRequest.name().trim());
            spec.setPriceDelta(specRequest.priceDelta());
            spec.setStock(specRequest.stock());
            spec.setCalories(specRequest.calories());
            spec.setIsDefault(i == defaultIndex ? 1 : 0);
            spec.setSort(specRequest.sort() == null ? 0 : specRequest.sort());
            spec.setStatus(specRequest.status() == null ? STATUS_ON : specRequest.status());
            specMapper.insert(spec);
            if (i == defaultIndex) {
                defaultSpecId = spec.getId();
            }
        }

        if (request.options() != null) {
            for (ProductOptionRequest optionRequest : sorted(request.options(), ProductOptionRequest::sort)) {
                ProductOption option = new ProductOption();
                option.setProductId(productId);
                option.setName(optionRequest.name().trim());
                option.setType(optionRequest.type());
                option.setRequired(optionRequest.required() == null ? 0 : optionRequest.required());
                option.setSort(optionRequest.sort() == null ? 0 : optionRequest.sort());
                optionMapper.insert(option);
                for (ProductOptionItemRequest itemRequest : sorted(optionRequest.items(), ProductOptionItemRequest::sort)) {
                    ProductOptionItem item = new ProductOptionItem();
                    item.setOptionId(option.getId());
                    item.setName(itemRequest.name().trim());
                    item.setPriceDelta(itemRequest.priceDelta());
                    item.setCalories(itemRequest.calories());
                    item.setSort(itemRequest.sort() == null ? 0 : itemRequest.sort());
                    item.setStatus(itemRequest.status() == null ? STATUS_ON : itemRequest.status());
                    optionItemMapper.insert(item);
                }
            }
        }

        if (request.images() != null) {
            for (ProductImageRequest imageRequest : sorted(request.images(), ProductImageRequest::sort)) {
                ProductImage image = new ProductImage();
                image.setProductId(productId);
                image.setImageUrl(imageRequest.imageUrl().trim());
                image.setSort(imageRequest.sort() == null ? 0 : imageRequest.sort());
                image.setStatus(imageRequest.status() == null ? STATUS_ON : imageRequest.status());
                imageMapper.insert(image);
            }
        }
        return defaultSpecId;
    }

    private void deleteChildren(Long productId) {
        List<Long> optionIds = optionMapper.selectList(Wrappers.<ProductOption>lambdaQuery()
                        .eq(ProductOption::getProductId, productId))
                .stream()
                .map(ProductOption::getId)
                .toList();
        if (!optionIds.isEmpty()) {
            optionItemMapper.delete(Wrappers.<ProductOptionItem>lambdaQuery()
                    .in(ProductOptionItem::getOptionId, optionIds));
        }
        optionMapper.delete(Wrappers.<ProductOption>lambdaQuery()
                .eq(ProductOption::getProductId, productId));
        specMapper.delete(Wrappers.<ProductSpec>lambdaQuery()
                .eq(ProductSpec::getProductId, productId));
        imageMapper.delete(Wrappers.<ProductImage>lambdaQuery()
                .eq(ProductImage::getProductId, productId));
    }

    private List<ProductSpec> selectSpecs(Long productId, Integer status) {
        return specMapper.selectList(Wrappers.<ProductSpec>lambdaQuery()
                .eq(ProductSpec::getProductId, productId)
                .eq(status != null, ProductSpec::getStatus, status)
                .orderByAsc(ProductSpec::getSort)
                .orderByAsc(ProductSpec::getId));
    }

    /** status 为 null 时返回全部状态的选项组（管理端），否则仅返回启用子项的选项组（用户端）。 */
    private List<ProductOptionVO> selectOptionVos(Long productId, Integer status) {
        List<ProductOption> options = optionMapper.selectList(Wrappers.<ProductOption>lambdaQuery()
                .eq(ProductOption::getProductId, productId)
                .orderByAsc(ProductOption::getSort)
                .orderByAsc(ProductOption::getId));
        if (options.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ProductOptionItem>> itemsByOption = optionItemMapper.selectList(
                        Wrappers.<ProductOptionItem>lambdaQuery()
                                .in(ProductOptionItem::getOptionId, options.stream().map(ProductOption::getId).toList())
                                .eq(status != null, ProductOptionItem::getStatus, status)
                                .orderByAsc(ProductOptionItem::getSort)
                                .orderByAsc(ProductOptionItem::getId))
                .stream()
                .collect(Collectors.groupingBy(ProductOptionItem::getOptionId, LinkedHashMap::new, Collectors.toList()));
        return options.stream()
                .map(option -> new ProductOptionVO(
                        option.getId(),
                        option.getName(),
                        option.getType(),
                        option.getRequired(),
                        option.getSort(),
                        itemsByOption.getOrDefault(option.getId(), List.of()).stream()
                                .map(item -> new ProductOptionItemVO(
                                        item.getId(),
                                        item.getName(),
                                        item.getPriceDelta(),
                                        item.getCalories(),
                                        item.getSort(),
                                        item.getStatus()))
                                .toList()))
                .toList();
    }

    private List<ProductImageVO> selectImageVos(Long productId, Integer status) {
        return imageMapper.selectList(Wrappers.<ProductImage>lambdaQuery()
                        .eq(ProductImage::getProductId, productId)
                        .eq(status != null, ProductImage::getStatus, status)
                        .orderByAsc(ProductImage::getSort)
                        .orderByAsc(ProductImage::getId))
                .stream()
                .map(image -> new ProductImageVO(image.getId(), image.getImageUrl(), image.getSort(), image.getStatus()))
                .toList();
    }

    private List<ProductSpecVO> toSpecVos(List<ProductSpec> specs) {
        return specs.stream()
                .map(spec -> new ProductSpecVO(
                        spec.getId(),
                        spec.getName(),
                        spec.getPriceDelta(),
                        spec.getStock(),
                        spec.getCalories(),
                        spec.getIsDefault(),
                        spec.getSort(),
                        spec.getStatus()))
                .toList();
    }

    // ---------- 查询辅助 ----------

    private Product requireProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND.getCode(),
                    ErrorCode.PRODUCT_NOT_FOUND.getMessage());
        }
        return product;
    }

    private void requireCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND.getCode(),
                    ErrorCode.CATEGORY_NOT_FOUND.getMessage());
        }
    }

    /** 从 rootId 起按父子结构收集子树 ID（含 rootId 自身）。 */
    private List<Long> subtreeIds(Long rootId, List<Category> categories) {
        Map<Long, List<Long>> childrenByParent = categories.stream()
                .filter(category -> category.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId, LinkedHashMap::new,
                        Collectors.mapping(Category::getId, Collectors.toList())));
        List<Long> result = new ArrayList<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(rootId);
        while (!stack.isEmpty()) {
            Long current = stack.pop();
            result.add(current);
            for (Long childId : childrenByParent.getOrDefault(current, List.of())) {
                stack.push(childId);
            }
        }
        return result;
    }

    private Set<Long> enabledCategoryIds() {
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                        .eq(Category::getStatus, STATUS_ON))
                .stream()
                .map(Category::getId)
                .collect(Collectors.toSet());
    }

    private String categoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryMapper.selectById(categoryId);
        return category != null ? category.getName() : null;
    }

    private Map<Long, String> categoryNameMap(Collection<Product> products) {
        Set<Long> categoryIds = products.stream()
                .map(Product::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }

    private int clampLimit(int limit) {
        return Math.max(1, Math.min(limit, MAX_LIST_LIMIT));
    }

    private ShopProductVO toShopVo(Product product, String categoryName) {
        return new ShopProductVO(
                product.getId(),
                product.getCategoryId(),
                categoryName,
                product.getName(),
                product.getSubtitle(),
                product.getCover(),
                product.getUnit(),
                product.getPrice(),
                product.getOriginalPrice(),
                product.getCalories(),
                product.getProtein(),
                product.getFat(),
                product.getCarbohydrate(),
                product.getSodium(),
                splitCsv(product.getHealthTags()),
                product.getStock(),
                product.getSalesCount(),
                product.getStatus(),
                product.getStatus() == STATUS_ON && product.getStock() != null && product.getStock() > 0);
    }

    private AdminProductVO toAdminVo(Product product, String categoryName) {
        return new AdminProductVO(
                product.getId(),
                product.getCategoryId(),
                categoryName,
                product.getName(),
                product.getSubtitle(),
                product.getCover(),
                product.getUnit(),
                product.getPrice(),
                product.getOriginalPrice(),
                product.getStock(),
                product.getSalesCount(),
                product.getStatus(),
                product.getIsRecommend(),
                product.getSort(),
                product.getCalories(),
                splitCsv(product.getHealthTags()),
                product.getUpdateTime());
    }

    // ---------- 通用小工具 ----------

    private <T> List<T> sorted(List<T> items, Function<T, Integer> sortGetter) {
        return items.stream()
                .sorted(Comparator.comparing(sortGetter, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
    }

    private String storageLabel(Integer storageType) {
        if (storageType == null) {
            return null;
        }
        return switch (storageType) {
            case 1 -> "常温";
            case 2 -> "冷藏";
            case 3 -> "冷冻";
            default -> null;
        };
    }

    private List<String> splitCsv(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String joinCsv(List<String> values, int maxLength, String label) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        String joined = values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(","));
        if (joined.isEmpty()) {
            return null;
        }
        if (joined.length() > maxLength) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    label + "总长度不能超过" + maxLength + "个字符");
        }
        return joined;
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
