package com.freshmeal.service;

import com.freshmeal.common.PageResult;
import com.freshmeal.vo.user.ShopProductVO;
import java.util.List;

public interface EngagementService {

    List<Long> favoriteIds(Long userId);

    boolean toggleFavorite(Long userId, Long productId);

    PageResult<ShopProductVO> favoritePage(Long userId, int current, int size);

    void recordHistory(Long userId, Long productId);

    PageResult<ShopProductVO> historyPage(Long userId, int current, int size);
}
