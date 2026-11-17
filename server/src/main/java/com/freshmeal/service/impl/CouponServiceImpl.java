package com.freshmeal.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.freshmeal.common.BusinessException;
import com.freshmeal.common.PageResult;
import com.freshmeal.dto.admin.CouponSaveRequest;
import com.freshmeal.entity.Coupon;
import com.freshmeal.entity.UserCoupon;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.mapper.CouponMapper;
import com.freshmeal.mapper.UserCouponMapper;
import com.freshmeal.service.CouponService;
import com.freshmeal.vo.admin.AdminCouponVO;
import com.freshmeal.vo.user.CouponVO;
import com.freshmeal.vo.user.UserCouponVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private static final int COUPON_ACTIVE = 1;
    private static final int COUPON_DISABLED = 0;
    private static final int USER_COUPON_UNUSED = 0;
    private static final int USER_COUPON_USED = 1;
    private static final int TYPE_AMOUNT = 1;
    private static final int TYPE_DISCOUNT = 2;

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    @Override
    public List<CouponVO> listAvailable(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> coupons = couponMapper.selectList(Wrappers.<Coupon>lambdaQuery()
                .eq(Coupon::getStatus, COUPON_ACTIVE)
                .le(Coupon::getStartTime, now)
                .ge(Coupon::getEndTime, now)
                .orderByAsc(Coupon::getThreshold)
                .orderByDesc(Coupon::getId));
        if (coupons.isEmpty()) {
            return List.of();
        }
        Set<Long> receivedCouponIds = userCouponMapper.selectList(
                        Wrappers.<UserCoupon>lambdaQuery()
                                .eq(UserCoupon::getUserId, userId))
                .stream()
                .map(UserCoupon::getCouponId)
                .collect(Collectors.toSet());
        return coupons.stream()
                .filter(coupon -> coupon.getReceivedCount() < coupon.getTotalCount())
                .filter(coupon -> !receivedCouponIds.contains(coupon.getId()))
                .map(coupon -> new CouponVO(
                        coupon.getId(),
                        coupon.getName(),
                        coupon.getType(),
                        coupon.getThreshold(),
                        coupon.getAmount(),
                        coupon.getStartTime(),
                        coupon.getEndTime(),
                        coupon.getTotalCount(),
                        coupon.getReceivedCount(),
                        coupon.getStatus()))
                .toList();
    }

    @Override
    public List<UserCouponVO> listUserCoupons(Long userId) {
        List<UserCoupon> userCoupons = userCouponMapper.selectList(
                Wrappers.<UserCoupon>lambdaQuery()
                        .eq(UserCoupon::getUserId, userId)
                        .orderByDesc(UserCoupon::getReceiveTime)
                        .orderByDesc(UserCoupon::getId));
        if (userCoupons.isEmpty()) {
            return List.of();
        }
        List<Coupon> coupons = couponMapper.selectBatchIds(
                userCoupons.stream().map(UserCoupon::getCouponId).distinct().toList());
        return userCoupons.stream()
                .map(userCoupon -> toVo(userCoupon, findCoupon(coupons, userCoupon.getCouponId())))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCouponVO receive(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        validateActiveCoupon(coupon);
        validateNotReceived(userId, couponId);
        int updated = couponMapper.update(null,
                Wrappers.<Coupon>lambdaUpdate()
                        .eq(Coupon::getId, couponId)
                        .eq(Coupon::getStatus, COUPON_ACTIVE)
                        .lt(Coupon::getReceivedCount, coupon.getTotalCount())
                        .setSql("received_count = received_count + 1"));
        if (updated != 1) {
            throw new BusinessException(ErrorCode.COUPON_UNAVAILABLE.getCode(),
                    "优惠券已领完或已停用");
        }
        return insertUserCoupon(userId, coupon);
    }

    @Override
    public BigDecimal calculateDiscount(Long userId, Long userCouponId,
            BigDecimal goodsAmount) {
        if (userCouponId == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        UserCoupon userCoupon = requireUserCoupon(userId, userCouponId);
        Coupon coupon = requireCoupon(userCoupon.getCouponId());
        validateUsableCoupon(coupon);
        if (userCoupon.getStatus() != USER_COUPON_UNUSED
                || userCoupon.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.COUPON_UNAVAILABLE.getCode(),
                    "优惠券已使用或已过期");
        }
        if (goodsAmount.compareTo(coupon.getThreshold()) < 0) {
            throw new BusinessException(ErrorCode.COUPON_UNAVAILABLE.getCode(),
                    String.format("订单商品金额未达到 %.2f 元使用门槛",
                            coupon.getThreshold()));
        }
        BigDecimal discount;
        if (coupon.getType() == TYPE_AMOUNT) {
            discount = coupon.getAmount();
        } else if (coupon.getType() == TYPE_DISCOUNT
                && coupon.getAmount().compareTo(BigDecimal.ZERO) > 0
                && coupon.getAmount().compareTo(BigDecimal.ONE) < 0) {
            discount = goodsAmount.multiply(BigDecimal.ONE.subtract(coupon.getAmount()));
        } else {
            throw new BusinessException(ErrorCode.COUPON_UNAVAILABLE.getCode(),
                    "优惠券配置不合法");
        }
        return discount.min(goodsAmount).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markUsed(Long userCouponId, Long orderId) {
        if (userCouponId == null) {
            return;
        }
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || userCoupon.getStatus() != USER_COUPON_UNUSED) {
            throw new BusinessException(ErrorCode.COUPON_UNAVAILABLE.getCode(),
                    "优惠券已使用或不存在");
        }
        int updated = userCouponMapper.update(null,
                Wrappers.<UserCoupon>lambdaUpdate()
                        .eq(UserCoupon::getId, userCouponId)
                        .eq(UserCoupon::getStatus, USER_COUPON_UNUSED)
                        .set(UserCoupon::getStatus, USER_COUPON_USED)
                        .set(UserCoupon::getOrderId, orderId)
                        .set(UserCoupon::getUseTime, LocalDateTime.now()));
        if (updated != 1) {
            throw new BusinessException(ErrorCode.COUPON_UNAVAILABLE.getCode(),
                    "优惠券状态已变化，请刷新后重试");
        }
        couponMapper.update(null,
                Wrappers.<Coupon>lambdaUpdate()
                        .eq(Coupon::getId, userCoupon.getCouponId())
                        .setSql("used_count = used_count + 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void release(Long userCouponId) {
        if (userCouponId == null) {
            return;
        }
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || userCoupon.getStatus() != USER_COUPON_USED) {
            return;
        }
        int updated = userCouponMapper.update(null,
                Wrappers.<UserCoupon>lambdaUpdate()
                        .eq(UserCoupon::getId, userCouponId)
                        .eq(UserCoupon::getStatus, USER_COUPON_USED)
                        .set(UserCoupon::getStatus, USER_COUPON_UNUSED)
                        .set(UserCoupon::getOrderId, null)
                        .set(UserCoupon::getUseTime, null));
        if (updated == 1) {
            couponMapper.update(null,
                    Wrappers.<Coupon>lambdaUpdate()
                            .eq(Coupon::getId, userCoupon.getCouponId())
                            .gt(Coupon::getUsedCount, 0)
                            .setSql("used_count = used_count - 1"));
        }
    }

    @Override
    public PageResult<AdminCouponVO> pageAdmin() {
        List<Coupon> coupons = couponMapper.selectList(
                Wrappers.<Coupon>lambdaQuery()
                        .orderByDesc(Coupon::getCreateTime)
                        .orderByDesc(Coupon::getId));
        List<AdminCouponVO> records = coupons.stream().map(coupon -> new AdminCouponVO(
                coupon.getId(),
                coupon.getName(),
                coupon.getType(),
                coupon.getThreshold(),
                coupon.getAmount(),
                coupon.getStartTime(),
                coupon.getEndTime(),
                coupon.getTotalCount(),
                coupon.getReceivedCount(),
                coupon.getUsedCount(),
                coupon.getStatus())).toList();
        return PageResult.of(records, records.size(), 1, Math.max(records.size(), 1));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CouponSaveRequest request) {
        validateRequest(request);
        Coupon coupon = new Coupon();
        coupon.setName(request.name().trim());
        coupon.setType(request.type());
        coupon.setThreshold(request.threshold());
        coupon.setAmount(request.amount());
        coupon.setStartTime(request.startTime());
        coupon.setEndTime(request.endTime());
        coupon.setTotalCount(request.totalCount());
        coupon.setReceivedCount(0);
        coupon.setUsedCount(0);
        coupon.setStatus(COUPON_ACTIVE);
        couponMapper.insert(coupon);
        return coupon.getId();
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != COUPON_ACTIVE && status != COUPON_DISABLED)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "优惠券状态不合法");
        }
        int updated = couponMapper.update(null,
                Wrappers.<Coupon>lambdaUpdate()
                        .eq(Coupon::getId, id)
                        .set(Coupon::getStatus, status));
        if (updated != 1) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND.getCode(),
                    ErrorCode.COUPON_NOT_FOUND.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void issue(Long couponId, Long userId) {
        receive(userId, couponId);
    }

    private void validateRequest(CouponSaveRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "优惠券失效时间必须晚于生效时间");
        }
        if (request.type() == TYPE_DISCOUNT
                && (request.amount().compareTo(BigDecimal.ZERO) <= 0
                || request.amount().compareTo(BigDecimal.ONE) >= 0)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "折扣券金额必须在0到1之间");
        }
        if (request.type() == TYPE_AMOUNT && request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "满减券优惠金额必须大于0");
        }
    }

    private void validateActiveCoupon(Coupon coupon) {
        if (coupon == null) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND.getCode(),
                    ErrorCode.COUPON_NOT_FOUND.getMessage());
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStatus() != COUPON_ACTIVE
                || coupon.getStartTime().isAfter(now)
                || coupon.getEndTime().isBefore(now)
                || coupon.getReceivedCount() >= coupon.getTotalCount()) {
            throw new BusinessException(ErrorCode.COUPON_UNAVAILABLE.getCode(),
                    "优惠券当前不可领取");
        }
    }

    /** A coupon already in the user's wallet stays usable even once the
     *  template is sold out, so stock is not part of this check. */
    private void validateUsableCoupon(Coupon coupon) {
        if (coupon == null) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND.getCode(),
                    ErrorCode.COUPON_NOT_FOUND.getMessage());
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStatus() != COUPON_ACTIVE
                || coupon.getStartTime().isAfter(now)
                || coupon.getEndTime().isBefore(now)) {
            throw new BusinessException(ErrorCode.COUPON_UNAVAILABLE.getCode(),
                    "优惠券当前不可使用");
        }
    }

    private void validateNotReceived(Long userId, Long couponId) {
        Long claimed = userCouponMapper.selectCount(
                Wrappers.<UserCoupon>lambdaQuery()
                        .eq(UserCoupon::getUserId, userId)
                        .eq(UserCoupon::getCouponId, couponId));
        if (claimed != null && claimed > 0) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_RECEIVED.getCode(),
                    ErrorCode.COUPON_ALREADY_RECEIVED.getMessage());
        }
    }

    private UserCouponVO insertUserCoupon(Long userId, Coupon coupon) {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(coupon.getId());
        userCoupon.setStatus(USER_COUPON_UNUSED);
        userCoupon.setReceiveTime(LocalDateTime.now());
        userCoupon.setExpireTime(coupon.getEndTime());
        try {
            userCouponMapper.insert(userCoupon);
        } catch (DuplicateKeyException exception) {
            // Concurrent claims for the same template: the unique key is the
            // final guard, so report it as a normal business rejection.
            throw new BusinessException(ErrorCode.COUPON_ALREADY_RECEIVED.getCode(),
                    ErrorCode.COUPON_ALREADY_RECEIVED.getMessage());
        }
        return toVo(userCoupon, coupon);
    }

    private UserCoupon requireUserCoupon(Long userId, Long userCouponId) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !userId.equals(userCoupon.getUserId())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND.getCode(),
                    ErrorCode.COUPON_NOT_FOUND.getMessage());
        }
        return userCoupon;
    }

    private Coupon requireCoupon(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND.getCode(),
                    ErrorCode.COUPON_NOT_FOUND.getMessage());
        }
        return coupon;
    }

    private Coupon findCoupon(List<Coupon> coupons, Long couponId) {
        return coupons.stream()
                .filter(coupon -> couponId.equals(coupon.getId()))
                .findFirst()
                .orElse(null);
    }

    private UserCouponVO toVo(UserCoupon userCoupon, Coupon coupon) {
        if (coupon == null) {
            return new UserCouponVO(userCoupon.getId(), userCoupon.getCouponId(),
                    "已失效优惠券", TYPE_AMOUNT, BigDecimal.ZERO, BigDecimal.ZERO,
                    userCoupon.getStatus(), userCoupon.getOrderId(),
                    userCoupon.getReceiveTime(), userCoupon.getUseTime(),
                    userCoupon.getExpireTime());
        }
        return new UserCouponVO(userCoupon.getId(), userCoupon.getCouponId(),
                coupon.getName(), coupon.getType(), coupon.getThreshold(),
                coupon.getAmount(), userCoupon.getStatus(), userCoupon.getOrderId(),
                userCoupon.getReceiveTime(), userCoupon.getUseTime(),
                userCoupon.getExpireTime());
    }
}
