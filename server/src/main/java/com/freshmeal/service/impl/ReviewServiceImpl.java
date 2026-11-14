package com.freshmeal.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshmeal.common.BusinessException;
import com.freshmeal.common.PageResult;
import com.freshmeal.dto.admin.AdminReviewQuery;
import com.freshmeal.dto.user.ReviewSubmitRequest;
import com.freshmeal.entity.Order;
import com.freshmeal.entity.OrderItem;
import com.freshmeal.entity.Product;
import com.freshmeal.entity.Review;
import com.freshmeal.entity.User;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.enums.OrderStatus;
import com.freshmeal.mapper.OrderItemMapper;
import com.freshmeal.mapper.OrderMapper;
import com.freshmeal.mapper.ProductMapper;
import com.freshmeal.mapper.ReviewMapper;
import com.freshmeal.mapper.UserMapper;
import com.freshmeal.service.ReviewService;
import com.freshmeal.vo.admin.AdminReviewVO;
import com.freshmeal.vo.user.ReviewVO;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewVO submit(Long userId, String orderNo, ReviewSubmitRequest request) {
        Order order = requireOwnedOrder(userId, orderNo);
        if (order.getStatus() != OrderStatus.FINISHED.getCode()) {
            throw new BusinessException(ErrorCode.ORDER_STATE_INVALID.getCode(),
                    "订单完成后才能评价");
        }
        boolean belongsToOrder = orderItemMapper.exists(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, order.getId())
                .eq(OrderItem::getProductId, request.productId()));
        if (!belongsToOrder) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "评价商品不在该订单中");
        }
        Long exists = reviewMapper.selectCount(Wrappers.<Review>lambdaQuery()
                .eq(Review::getOrderId, order.getId())
                .eq(Review::getProductId, request.productId()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(),
                    "该商品已经评价过");
        }
        Review review = new Review();
        review.setUserId(userId);
        review.setOrderId(order.getId());
        review.setProductId(request.productId());
        review.setRating(request.rating());
        review.setContent(StringUtils.hasText(request.content())
                ? request.content().trim() : null);
        review.setImages(writeImages(request.images()));
        reviewMapper.insert(review);
        return toUserVo(review, productName(review.getProductId()));
    }

    @Override
    public List<ReviewVO> listByOrder(Long userId, String orderNo) {
        Order order = requireOwnedOrder(userId, orderNo);
        List<Review> reviews = reviewMapper.selectList(
                Wrappers.<Review>lambdaQuery()
                        .eq(Review::getOrderId, order.getId())
                        .orderByDesc(Review::getCreateTime));
        Map<Long, String> productNames = loadProductNames(reviews);
        return reviews.stream()
                .map(review -> toUserVo(review,
                        productNames.getOrDefault(review.getProductId(), "已删除商品")))
                .toList();
    }

    @Override
    public PageResult<AdminReviewVO> pageAdmin(AdminReviewQuery query) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Review> wrapper =
                Wrappers.<Review>lambdaQuery()
                        .eq(query.getRating() != null, Review::getRating,
                                query.getRating())
                        .orderByDesc(Review::getCreateTime)
                        .orderByDesc(Review::getId);
        if (StringUtils.hasText(query.getOrderNo())) {
            List<Long> orderIds = orderMapper.selectList(
                            Wrappers.<Order>lambdaQuery()
                                    .like(Order::getOrderNo, query.getOrderNo().trim()))
                    .stream()
                    .map(Order::getId)
                    .toList();
            if (orderIds.isEmpty()) {
                return PageResult.of(List.of(), 0, query.getCurrent(), query.getSize());
            }
            wrapper.in(Review::getOrderId, orderIds);
        }
        Page<Review> page = reviewMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                wrapper);
        List<Review> reviews = page.getRecords();
        Map<Long, Order> orders = loadOrders(reviews);
        Map<Long, User> users = loadUsers(reviews);
        Map<Long, String> productNames = loadProductNames(reviews);
        List<AdminReviewVO> records = reviews.stream()
                .map(review -> {
                    Order order = orders.get(review.getOrderId());
                    User user = users.get(review.getUserId());
                    return new AdminReviewVO(
                            review.getId(),
                            review.getUserId(),
                            user == null ? null : user.getUsername(),
                            user == null ? null : user.getNickname(),
                            review.getOrderId(),
                            order == null ? null : order.getOrderNo(),
                            review.getProductId(),
                            productNames.getOrDefault(review.getProductId(), "已删除商品"),
                            review.getRating(),
                            review.getContent(),
                            readImages(review.getImages()),
                            review.getReply(),
                            review.getCreateTime());
                })
                .toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public void reply(Long id, String reply) {
        int updated = reviewMapper.update(null,
                Wrappers.<Review>lambdaUpdate()
                        .eq(Review::getId, id)
                        .set(Review::getReply,
                                StringUtils.hasText(reply) ? reply.trim() : null));
        if (updated != 1) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND.getCode(),
                    ErrorCode.REVIEW_NOT_FOUND.getMessage());
        }
    }

    @Override
    public void delete(Long id) {
        if (reviewMapper.deleteById(id) == 0) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND.getCode(),
                    ErrorCode.REVIEW_NOT_FOUND.getMessage());
        }
    }

    private Order requireOwnedOrder(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderNo, orderNo));
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND.getCode(),
                    ErrorCode.ORDER_NOT_FOUND.getMessage());
        }
        return order;
    }

    private String productName(Long productId) {
        Product product = productMapper.selectById(productId);
        return product == null ? "已删除商品" : product.getName();
    }

    private Map<Long, String> loadProductNames(List<Review> reviews) {
        List<Long> ids = reviews.stream()
                .map(Review::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return productMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName,
                        (first, second) -> first));
    }

    private Map<Long, Order> loadOrders(List<Review> reviews) {
        List<Long> ids = reviews.stream()
                .map(Review::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return orderMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity(),
                        (first, second) -> first));
    }

    private Map<Long, User> loadUsers(List<Review> reviews) {
        List<Long> ids = reviews.stream()
                .map(Review::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(),
                        (first, second) -> first));
    }

    private String writeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(images);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "评价图片格式不合法");
        }
    }

    private List<String> readImages(String images) {
        if (!StringUtils.hasText(images)) {
            return List.of();
        }
        try {
            String[] values = objectMapper.readValue(images, String[].class);
            return List.of(values);
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private ReviewVO toUserVo(Review review, String productName) {
        return new ReviewVO(
                review.getId(),
                review.getUserId(),
                review.getProductId(),
                productName,
                review.getRating(),
                review.getContent(),
                readImages(review.getImages()),
                review.getReply(),
                review.getCreateTime());
    }
}
