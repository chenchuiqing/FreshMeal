package com.freshmeal.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.freshmeal.common.BusinessException;
import com.freshmeal.dto.user.CartItemAddRequest;
import com.freshmeal.dto.user.CartItemUpdateRequest;
import com.freshmeal.entity.Cart;
import com.freshmeal.entity.Product;
import com.freshmeal.entity.ProductOption;
import com.freshmeal.entity.ProductOptionItem;
import com.freshmeal.entity.ProductSpec;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.mapper.CartMapper;
import com.freshmeal.mapper.ProductMapper;
import com.freshmeal.mapper.ProductOptionItemMapper;
import com.freshmeal.mapper.ProductOptionMapper;
import com.freshmeal.mapper.ProductSpecMapper;
import com.freshmeal.service.CartService;
import com.freshmeal.service.PricedCartLine;
import com.freshmeal.vo.user.CartLineVO;
import com.freshmeal.vo.user.CartOptionVO;
import com.freshmeal.vo.user.CartSummaryVO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final int MAX_QUANTITY_PER_LINE = 99;
    private static final TypeReference<List<CartOptionVO>> OPTION_TYPE =
            new TypeReference<>() {
            };

    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final ProductSpecMapper productSpecMapper;
    private final ProductOptionMapper productOptionMapper;
    private final ProductOptionItemMapper productOptionItemMapper;
    private final ObjectMapper objectMapper;

    @Override
    public CartSummaryVO list(Long userId) {
        List<CartLineVO> lines = cartMapper.selectList(Wrappers.<Cart>lambdaQuery()
                        .eq(Cart::getUserId, userId)
                        .orderByDesc(Cart::getCreateTime)
                        .orderByDesc(Cart::getId))
                .stream()
                .map(this::toLine)
                .toList();
        lines = applyAggregateAvailability(lines);

        BigDecimal goodsAmount = lines.stream()
                .filter(CartLineVO::checked)
                .map(CartLineVO::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int calories = lines.stream()
                .filter(CartLineVO::checked)
                .mapToInt(CartLineVO::subtotalCalories)
                .sum();
        int checkedCount = (int) lines.stream().filter(CartLineVO::checked).count();
        return new CartSummaryVO(lines, goodsAmount, calories, checkedCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartLineVO add(Long userId, CartItemAddRequest request) {
        Product product = requirePurchasableProduct(request.productId());
        List<Cart> existingRows = cartMapper.selectList(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getUserId, userId));
        Selection selection = validateSelection(product, request.specId(),
                nullToEmpty(request.optionItemIds()), existingRows);
        String signature = signature(
                selection.spec() == null ? null : selection.spec().getId(),
                selection.options());

        Cart merged = existingRows.stream()
                .filter(row -> Objects.equals(row.getProductId(), product.getId()))
                .filter(row -> signature.equals(signature(row.getSpecId(), readOptions(row))))
                .findFirst()
                .orElse(null);
        Long excludeId = merged == null ? null : merged.getId();
        int projectedQuantity = request.quantity()
                + quantityForProduct(existingRows, product.getId(), excludeId);
        requireProjectedStock(product, projectedQuantity);

        if (merged != null) {
            int totalQuantity = merged.getQuantity() + request.quantity();
            if (totalQuantity > MAX_QUANTITY_PER_LINE) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                        "同一规格购物车数量最多为99");
            }
            merged.setQuantity(totalQuantity);
            merged.setChecked(1);
            merged.setPrice(unitPrice(product, selection));
            cartMapper.updateById(merged);
            return toLine(merged);
        }

        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setProductId(product.getId());
        cart.setSpecId(selection.spec() == null ? null : selection.spec().getId());
        cart.setQuantity(request.quantity());
        cart.setOptions(writeOptions(selection.options()));
        cart.setChecked(1);
        cart.setProductName(product.getName());
        cart.setCover(product.getCover());
        cart.setPrice(unitPrice(product, selection));
        cartMapper.insert(cart);
        return toLine(cart);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartLineVO update(Long userId, Long cartId, CartItemUpdateRequest request) {
        Cart cart = requireOwned(userId, cartId);
        Product product = requirePurchasableProduct(cart.getProductId());
        List<Cart> existingRows = cartMapper.selectList(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getUserId, userId));

        Long specId = request.specId() != null ? request.specId() : cart.getSpecId();
        List<Long> optionIds = request.optionItemIds() != null
                ? request.optionItemIds()
                : readOptions(cart).stream().map(CartOptionVO::itemId).toList();
        Selection selection = validateSelection(product, specId, optionIds, existingRows);
        int quantity = request.quantity() == null ? cart.getQuantity() : request.quantity();
        requireProjectedStock(product,
                quantity + quantityForProduct(existingRows, product.getId(), cartId));

        cart.setSpecId(selection.spec() == null ? null : selection.spec().getId());
        cart.setQuantity(quantity);
        cart.setOptions(writeOptions(selection.options()));
        cart.setChecked(request.checked() == null
                ? cart.getChecked()
                : Boolean.TRUE.equals(request.checked()) ? 1 : 0);
        cart.setProductName(product.getName());
        cart.setCover(product.getCover());
        cart.setPrice(unitPrice(product, selection));
        cartMapper.updateById(cart);
        return toLine(cart);
    }

    @Override
    public void delete(Long userId, Long cartId) {
        Cart cart = requireOwned(userId, cartId);
        cartMapper.deleteById(cart.getId());
    }

    @Override
    public List<PricedCartLine> priceSelected(Long userId, List<Long> cartItemIds,
            boolean requireChecked) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_ITEM_UNAVAILABLE.getCode(),
                    ErrorCode.CART_ITEM_UNAVAILABLE.getMessage());
        }
        List<Long> uniqueIds = cartItemIds.stream().filter(Objects::nonNull).distinct().toList();
        List<Cart> carts = cartMapper.selectBatchIds(uniqueIds);
        if (carts.size() != uniqueIds.size()
                || carts.stream().anyMatch(cart -> !userId.equals(cart.getUserId())
                || requireChecked && cart.getChecked() != 1)) {
            throw new BusinessException(ErrorCode.CART_ITEM_UNAVAILABLE.getCode(),
                    ErrorCode.CART_ITEM_UNAVAILABLE.getMessage());
        }

        Map<Long, List<PricedCartLine>> linesByProduct = carts.stream()
                .map(this::toPricedLine)
                .collect(Collectors.groupingBy(line -> line.product().getId(),
                        LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<Long, List<PricedCartLine>> entry : linesByProduct.entrySet()) {
            Product product = entry.getValue().get(0).product();
            int requestedQuantity = entry.getValue().stream()
                    .mapToInt(line -> line.line().quantity())
                    .sum();
            if (product.getStatus() == null || product.getStatus() != 1
                    || product.getStock() == null || product.getStock() < requestedQuantity) {
                throw new BusinessException(ErrorCode.CART_ITEM_UNAVAILABLE.getCode(),
                        ErrorCode.CART_ITEM_UNAVAILABLE.getMessage());
            }
        }
        return carts.stream()
                .map(this::toPricedLine)
                .toList();
    }

    private PricedCartLine toPricedLine(Cart cart) {
        Product product = productMapper.selectById(cart.getProductId());
        if (product == null) {
            throw new BusinessException(ErrorCode.CART_ITEM_UNAVAILABLE.getCode(),
                    ErrorCode.CART_ITEM_UNAVAILABLE.getMessage());
        }
        List<Long> optionIds = readOptions(cart).stream()
                .map(CartOptionVO::itemId)
                .toList();
        Selection selection = validateSelection(product, cart.getSpecId(), optionIds, List.of());
        CartLineVO line = buildLine(cart, product, selection);
        if (!Objects.equals(cart.getPrice(), line.unitPrice())
                || !Objects.equals(cart.getOptions(), writeOptions(selection.options()))) {
            cart.setPrice(line.unitPrice());
            cart.setOptions(writeOptions(selection.options()));
            cartMapper.updateById(cart);
        }
        if (product.getStatus() == null || product.getStatus() != 1
                || product.getStock() == null || product.getStock() < cart.getQuantity()) {
            throw new BusinessException(ErrorCode.CART_ITEM_UNAVAILABLE.getCode(),
                    ErrorCode.CART_ITEM_UNAVAILABLE.getMessage());
        }
        return new PricedCartLine(line, product,
                selection.spec() == null ? null : selection.spec());
    }

    private CartLineVO toLine(Cart cart) {
        Product product = productMapper.selectById(cart.getProductId());
        if (product == null) {
            return unavailableSnapshot(cart);
        }
        Selection selection;
        try {
            List<Long> optionIds = readOptions(cart).stream()
                    .map(CartOptionVO::itemId)
                    .toList();
            selection = validateSelection(
                    product, cart.getSpecId(), optionIds, List.of());
        } catch (BusinessException exception) {
            return unavailableSnapshot(cart);
        }
        CartLineVO line = buildLine(cart, product, selection);
        if (!Objects.equals(cart.getPrice(), line.unitPrice())
                || !Objects.equals(cart.getOptions(), writeOptions(selection.options()))) {
            cart.setPrice(line.unitPrice());
            cart.setOptions(writeOptions(selection.options()));
            cartMapper.updateById(cart);
        }
        return line;
    }

    private List<CartLineVO> applyAggregateAvailability(List<CartLineVO> lines) {
        Map<Long, Integer> quantityByProduct = lines.stream()
                .collect(Collectors.groupingBy(CartLineVO::productId,
                        Collectors.summingInt(CartLineVO::quantity)));
        Map<Long, Integer> stockByProduct = lines.stream()
                .map(CartLineVO::productId)
                .distinct()
                .map(productMapper::selectById)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Product::getId,
                        product -> product.getStock() == null ? 0 : product.getStock(),
                        (first, second) -> first));
        return lines.stream()
                .map(line -> line.available()
                        && stockByProduct.getOrDefault(line.productId(), 0)
                        >= quantityByProduct.getOrDefault(line.productId(), 0)
                        ? line
                        : new CartLineVO(line.id(), line.productId(), line.productName(),
                        line.cover(), line.unit(), line.specId(), line.specName(),
                        line.options(), line.optionsDesc(), line.unitPrice(), line.subtotal(),
                        line.calories(), line.subtotalCalories(), line.quantity(),
                        line.checked(), false))
                .toList();
    }

    private CartLineVO buildLine(Cart cart, Product product, Selection selection) {
        BigDecimal unitPrice = unitPrice(product, selection);
        int calories = selection.spec() == null
                ? product.getCalories()
                : selection.spec().getCalories();
        calories += selection.options().stream()
                .mapToInt(CartOptionVO::calories)
                .sum();
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(cart.getQuantity()));
        boolean available = product.getStatus() != null && product.getStatus() == 1
                && product.getStock() != null && product.getStock() >= cart.getQuantity();
        return new CartLineVO(
                cart.getId(),
                product.getId(),
                product.getName(),
                product.getCover(),
                product.getUnit(),
                selection.spec() == null ? null : selection.spec().getId(),
                selection.spec() == null ? null : selection.spec().getName(),
                selection.options(),
                optionsDescription(selection.options()),
                unitPrice,
                subtotal,
                calories,
                calories * cart.getQuantity(),
                cart.getQuantity(),
                cart.getChecked() == 1,
                available
        );
    }

    private CartLineVO unavailableSnapshot(Cart cart) {
        List<CartOptionVO> options = readOptions(cart);
        BigDecimal subtotal = cart.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
        int calories = options.stream().mapToInt(CartOptionVO::calories).sum()
                * cart.getQuantity();
        return new CartLineVO(
                cart.getId(), cart.getProductId(), cart.getProductName(), cart.getCover(),
                "份", cart.getSpecId(), null, options, optionsDescription(options),
                cart.getPrice(), subtotal, calories, calories, cart.getQuantity(),
                cart.getChecked() == 1, false
        );
    }

    private Selection validateSelection(Product product, Long specId, List<Long> optionIds,
            List<Cart> existingRows) {
        ProductSpec spec;
        if (specId == null) {
            spec = productSpecMapper.selectOne(Wrappers.<ProductSpec>lambdaQuery()
                    .eq(ProductSpec::getProductId, product.getId())
                    .eq(ProductSpec::getStatus, 1)
                    .orderByDesc(ProductSpec::getIsDefault)
                    .orderByAsc(ProductSpec::getSort)
                    .last("LIMIT 1"));
        } else {
            spec = productSpecMapper.selectById(specId);
            if (spec == null || !Objects.equals(spec.getProductId(), product.getId())
                    || spec.getStatus() == null || spec.getStatus() != 1) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "商品规格不合法");
            }
        }

        List<ProductOption> groups = productOptionMapper.selectList(
                Wrappers.<ProductOption>lambdaQuery()
                        .eq(ProductOption::getProductId, product.getId())
                        .orderByAsc(ProductOption::getSort)
                        .orderByAsc(ProductOption::getId));
        Set<Long> requestedIds = optionIds == null
                ? Set.of()
                : optionIds.stream().filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, List<ProductOptionItem>> itemsByGroup = groups.isEmpty()
                ? Map.of()
                : productOptionItemMapper.selectList(Wrappers.<ProductOptionItem>lambdaQuery()
                        .in(ProductOptionItem::getOptionId,
                                groups.stream().map(ProductOption::getId).toList())
                        .eq(ProductOptionItem::getStatus, 1)
                        .orderByAsc(ProductOptionItem::getSort)
                        .orderByAsc(ProductOptionItem::getId))
                .stream()
                .collect(Collectors.groupingBy(ProductOptionItem::getOptionId,
                        LinkedHashMap::new, Collectors.toList()));

        Map<Long, Long> groupByItem = itemsByGroup.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(item -> Map.entry(item.getId(), entry.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (first, second) -> first));
        if (requestedIds.stream().anyMatch(itemId -> !groupByItem.containsKey(itemId))) {
            throw new BusinessException(ErrorCode.INVALID_OPTION.getCode(),
                    ErrorCode.INVALID_OPTION.getMessage());
        }

        List<CartOptionVO> selected = new ArrayList<>();
        for (ProductOption group : groups) {
            List<Long> selectedIds = requestedIds.stream()
                    .filter(itemId -> Objects.equals(groupByItem.get(itemId), group.getId()))
                    .toList();
            if (group.getType() != null && group.getType() == 1 && selectedIds.size() > 1) {
                throw new BusinessException(ErrorCode.INVALID_OPTION.getCode(),
                        group.getName() + "只能选择一项");
            }
            if (group.getRequired() != null && group.getRequired() == 1
                    && selectedIds.isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                        "请选择" + group.getName());
            }
            List<ProductOptionItem> groupItems = itemsByGroup.getOrDefault(group.getId(), List.of());
            for (Long itemId : selectedIds) {
                ProductOptionItem item = groupItems.stream()
                        .filter(candidate -> Objects.equals(candidate.getId(), itemId))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(
                                ErrorCode.INVALID_OPTION.getCode(),
                                ErrorCode.INVALID_OPTION.getMessage()));
                selected.add(new CartOptionVO(group.getId(), item.getId(), item.getName(),
                        item.getPriceDelta(), item.getCalories()));
            }
        }
        return new Selection(spec, List.copyOf(selected));
    }

    private Product requirePurchasableProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND.getCode(),
                    ErrorCode.PRODUCT_NOT_FOUND.getMessage());
        }
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE.getCode(),
                    ErrorCode.PRODUCT_UNAVAILABLE.getMessage());
        }
        return product;
    }

    private void requireProjectedStock(Product product, int projectedQuantity) {
        if (product.getStock() == null || product.getStock() < projectedQuantity) {
            throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT.getCode(),
                    ErrorCode.INVENTORY_INSUFFICIENT.getMessage());
        }
    }

    private int quantityForProduct(List<Cart> rows, Long productId, Long excludeId) {
        return rows.stream()
                .filter(row -> Objects.equals(row.getProductId(), productId))
                .filter(row -> excludeId == null || !Objects.equals(row.getId(), excludeId))
                .mapToInt(Cart::getQuantity)
                .sum();
    }

    private Cart requireOwned(Long userId, Long cartId) {
        Cart cart = cartMapper.selectById(cartId);
        if (cart == null || !userId.equals(cart.getUserId())) {
            throw new BusinessException(ErrorCode.CART_ITEM_UNAVAILABLE.getCode(),
                    ErrorCode.CART_ITEM_UNAVAILABLE.getMessage());
        }
        return cart;
    }

    private BigDecimal unitPrice(Product product, Selection selection) {
        BigDecimal price = product.getPrice();
        if (selection.spec() != null && selection.spec().getPriceDelta() != null) {
            price = price.add(selection.spec().getPriceDelta());
        }
        for (CartOptionVO option : selection.options()) {
            price = price.add(option.priceDelta());
        }
        return price;
    }

    private String optionsDescription(List<CartOptionVO> options) {
        if (options.isEmpty()) {
            return "";
        }
        return options.stream().map(CartOptionVO::name).collect(Collectors.joining("、"));
    }

    private String signature(Long specId, List<CartOptionVO> options) {
        String optionSignature = options.stream()
                .map(CartOptionVO::itemId)
                .map(String::valueOf)
                .sorted()
                .collect(Collectors.joining(","));
        return (specId == null ? "" : specId) + "|" + optionSignature;
    }

    private List<CartOptionVO> readOptions(Cart cart) {
        if (!StringUtils.hasText(cart.getOptions())) {
            return List.of();
        }
        try {
            return objectMapper.readValue(cart.getOptions(), OPTION_TYPE);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INVALID_OPTION.getCode(),
                    ErrorCode.INVALID_OPTION.getMessage());
        }
    }

    private String writeOptions(List<CartOptionVO> options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(),
                    ErrorCode.INTERNAL_ERROR.getMessage());
        }
    }

    private List<Long> nullToEmpty(List<Long> values) {
        return values == null ? List.of() : values;
    }

    private record Selection(ProductSpec spec, List<CartOptionVO> options) {
    }
}
