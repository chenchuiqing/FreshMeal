package com.freshmeal.service;

import com.freshmeal.common.PageResult;
import com.freshmeal.dto.admin.AdminReviewQuery;
import com.freshmeal.dto.user.ReviewSubmitRequest;
import com.freshmeal.vo.admin.AdminReviewVO;
import com.freshmeal.vo.user.ReviewVO;
import java.util.List;

public interface ReviewService {

    ReviewVO submit(Long userId, String orderNo, ReviewSubmitRequest request);

    List<ReviewVO> listByOrder(Long userId, String orderNo);

    PageResult<AdminReviewVO> pageAdmin(AdminReviewQuery query);

    void reply(Long id, String reply);

    void delete(Long id);
}
