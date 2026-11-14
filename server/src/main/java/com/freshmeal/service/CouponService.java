package com.freshmeal.service;

import com.freshmeal.common.PageResult;
import com.freshmeal.dto.admin.CouponSaveRequest;
import com.freshmeal.vo.admin.AdminCouponVO;
import com.freshmeal.vo.user.CouponVO;
import com.freshmeal.vo.user.UserCouponVO;
import java.math.BigDecimal;
import java.util.List;

public interface CouponService {

    List<CouponVO> listAvailable();

    List<UserCouponVO> listUserCoupons(Long userId);

    UserCouponVO receive(Long userId, Long couponId);

    BigDecimal calculateDiscount(Long userId, Long userCouponId, BigDecimal goodsAmount);

    void markUsed(Long userCouponId, Long orderId);

    void release(Long userCouponId);

    PageResult<AdminCouponVO> pageAdmin();

    Long create(CouponSaveRequest request);

    void updateStatus(Long id, Integer status);

    void issue(Long couponId, Long userId);
}
