package com.freshmeal.service;

import com.freshmeal.common.PageResult;
import com.freshmeal.vo.admin.AdminRefundVO;
import com.freshmeal.vo.user.RefundVO;
import java.util.List;

public interface RefundService {

    void request(Long userId, String orderNo, String reason);

    List<RefundVO> listUser(Long userId);

    PageResult<AdminRefundVO> pageAdmin(int current, int size, Integer status);

    void decide(Long id, Integer status, String adminReply, Long adminId);
}
