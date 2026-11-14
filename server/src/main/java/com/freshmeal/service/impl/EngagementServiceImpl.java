package com.freshmeal.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.freshmeal.common.BusinessException;
import com.freshmeal.common.PageResult;
import com.freshmeal.entity.BrowsingHistory;
import com.freshmeal.entity.Favorite;
import com.freshmeal.entity.Product;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.mapper.BrowsingHistoryMapper;
import com.freshmeal.mapper.FavoriteMapper;
import com.freshmeal.mapper.ProductMapper;
import com.freshmeal.service.EngagementService;
import com.freshmeal.service.ProductService;
import com.freshmeal.vo.user.ShopProductVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EngagementServiceImpl implements EngagementService {

    private final FavoriteMapper favoriteMapper;
    private final BrowsingHistoryMapper historyMapper;
    private final ProductMapper productMapper;
    private final ProductService productService;

    @Override
    public List<Long> favoriteIds(Long userId) {
        return favoriteMapper.selectList(Wrappers.<Favorite>lambdaQuery()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime))
                .stream()
                .map(Favorite::getProductId)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFavorite(Long userId, Long productId) {
        requireProduct(productId);
        Favorite existing = favoriteMapper.selectOne(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            return false;
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        favorite.setCreateTime(LocalDateTime.now());
        try {
            favoriteMapper.insert(favorite);
        } catch (DuplicateKeyException exception) {
            return false;
        }
        return true;
    }

    @Override
    public PageResult<ShopProductVO> favoritePage(Long userId, int current, int size) {
        int safeSize = Math.max(size, 1);
        int safeCurrent = Math.max(current, 1);
        long total = favoriteMapper.selectCount(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId));
        List<Favorite> favorites = favoriteMapper.selectList(
                Wrappers.<Favorite>lambdaQuery()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime)
                        .orderByDesc(Favorite::getId)
                        .last("LIMIT " + safeSize + " OFFSET "
                                + ((safeCurrent - 1) * safeSize)));
        return toProductPage(favorites.stream().map(Favorite::getProductId).toList(),
                total, safeCurrent, safeSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordHistory(Long userId, Long productId) {
        requireProduct(productId);
        BrowsingHistory existing = historyMapper.selectOne(
                Wrappers.<BrowsingHistory>lambdaQuery()
                        .eq(BrowsingHistory::getUserId, userId)
                        .eq(BrowsingHistory::getProductId, productId));
        if (existing != null) {
            historyMapper.update(null,
                    Wrappers.<BrowsingHistory>lambdaUpdate()
                            .eq(BrowsingHistory::getId, existing.getId())
                            .set(BrowsingHistory::getViewTime, LocalDateTime.now()));
            return;
        }
        BrowsingHistory history = new BrowsingHistory();
        history.setUserId(userId);
        history.setProductId(productId);
        history.setViewTime(LocalDateTime.now());
        try {
            historyMapper.insert(history);
        } catch (DuplicateKeyException exception) {
            historyMapper.update(null,
                    Wrappers.<BrowsingHistory>lambdaUpdate()
                            .eq(BrowsingHistory::getUserId, userId)
                            .eq(BrowsingHistory::getProductId, productId)
                            .set(BrowsingHistory::getViewTime, LocalDateTime.now()));
        }
    }

    @Override
    public PageResult<ShopProductVO> historyPage(Long userId, int current, int size) {
        int safeSize = Math.max(size, 1);
        int safeCurrent = Math.max(current, 1);
        long total = historyMapper.selectCount(Wrappers.<BrowsingHistory>lambdaQuery()
                .eq(BrowsingHistory::getUserId, userId));
        List<BrowsingHistory> histories = historyMapper.selectList(
                Wrappers.<BrowsingHistory>lambdaQuery()
                        .eq(BrowsingHistory::getUserId, userId)
                        .orderByDesc(BrowsingHistory::getViewTime)
                        .orderByDesc(BrowsingHistory::getId)
                        .last("LIMIT " + safeSize + " OFFSET "
                                + ((safeCurrent - 1) * safeSize)));
        return toProductPage(histories.stream()
                .map(BrowsingHistory::getProductId)
                .toList(), total, safeCurrent, safeSize);
    }

    private void requireProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND.getCode(),
                    ErrorCode.PRODUCT_NOT_FOUND.getMessage());
        }
    }

    private PageResult<ShopProductVO> toProductPage(List<Long> productIds,
            long total, int current, int size) {
        if (productIds.isEmpty()) {
            return PageResult.of(List.of(), total, current, size);
        }
        Map<Long, ShopProductVO> products = productService
                .listShopProductsByIds(productIds)
                .stream()
                .collect(Collectors.toMap(ShopProductVO::id, Function.identity(),
                        (first, second) -> first));
        List<ShopProductVO> records = productIds.stream()
                .map(products::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        return PageResult.of(records, total, current, size);
    }
}
