package com.freshmeal.service;

import com.freshmeal.common.PageResult;
import com.freshmeal.dto.admin.AdminBatchLossRequest;
import com.freshmeal.dto.admin.AdminBatchQuery;
import com.freshmeal.dto.admin.AdminBatchSaveRequest;
import com.freshmeal.dto.admin.InventoryLogQuery;
import com.freshmeal.vo.admin.AdminBatchVO;
import com.freshmeal.vo.admin.AdminInventoryLogVO;
import java.util.List;

public interface AdminInventoryService {

    PageResult<AdminBatchVO> batches(AdminBatchQuery query);

    List<AdminBatchVO> expireWarning(int days);

    AdminBatchVO createBatch(AdminBatchSaveRequest request, Long adminId);

    AdminBatchVO reportLoss(Long batchId, AdminBatchLossRequest request, Long adminId);

    PageResult<AdminInventoryLogVO> logs(InventoryLogQuery query);
}
