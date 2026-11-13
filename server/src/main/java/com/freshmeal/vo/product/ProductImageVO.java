package com.freshmeal.vo.product;

public record ProductImageVO(
        Long id,
        String imageUrl,
        Integer sort,
        Integer status
) {
}
