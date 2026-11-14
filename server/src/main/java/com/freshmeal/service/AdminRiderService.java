package com.freshmeal.service;

import com.freshmeal.common.PageResult;
import com.freshmeal.dto.admin.AdminRiderCreateRequest;
import com.freshmeal.dto.admin.AdminRiderQuery;
import com.freshmeal.dto.admin.AdminRiderUpdateRequest;
import com.freshmeal.vo.admin.AdminRiderManageVO;
import java.util.List;

public interface AdminRiderService {

    PageResult<AdminRiderManageVO> page(AdminRiderQuery query);

    List<AdminRiderManageVO> list();

    AdminRiderManageVO create(AdminRiderCreateRequest request);

    AdminRiderManageVO update(Long id, AdminRiderUpdateRequest request);

    AdminRiderManageVO updateStatus(Long id, Integer status);

    AdminRiderManageVO updateWorkStatus(Long id, Integer onlineStatus, Integer workStatus);
}
