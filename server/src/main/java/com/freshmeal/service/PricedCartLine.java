package com.freshmeal.service;

import com.freshmeal.entity.Product;
import com.freshmeal.entity.ProductSpec;
import com.freshmeal.vo.user.CartLineVO;

public record PricedCartLine(
        CartLineVO line,
        Product product,
        ProductSpec spec
) {
}
