package com.freshmeal.vo.product;

import java.util.List;

public record CategoryVO(
        Long id,
        Long parentId,
        String name,
        String icon,
        Integer sort,
        Integer status,
        List<CategoryVO> children
) {
}
