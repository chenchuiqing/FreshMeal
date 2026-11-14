package com.freshmeal.service;

import com.freshmeal.common.PageResult;
import com.freshmeal.dto.admin.DispatchLogQuery;
import com.freshmeal.vo.admin.AdminDispatchLogVO;

public interface AdminDispatchLogService {

    PageResult<AdminDispatchLogVO> page(DispatchLogQuery query);
}
