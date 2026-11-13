package com.freshmeal.vo.product;

import java.util.List;

public record ProductOptionVO(
        Long id,
        String name,
        Integer type,
        Integer required,
        Integer sort,
        List<ProductOptionItemVO> items
) {
}
