package com.freshmeal.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freshmeal.common.BusinessException;
import com.freshmeal.common.PageResult;
import com.freshmeal.dto.admin.AdminBatchLossRequest;
import com.freshmeal.dto.admin.AdminBatchQuery;
import com.freshmeal.dto.admin.AdminBatchSaveRequest;
import com.freshmeal.dto.admin.InventoryLogQuery;
import com.freshmeal.entity.InventoryLog;
import com.freshmeal.entity.Product;
import com.freshmeal.entity.ProductBatch;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.mapper.InventoryLogMapper;
import com.freshmeal.mapper.ProductBatchMapper;
import com.freshmeal.mapper.ProductMapper;
import com.freshmeal.service.AdminInventoryService;
import com.freshmeal.vo.admin.AdminBatchVO;
import com.freshmeal.vo.admin.AdminInventoryLogVO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminInventoryServiceImpl implements AdminInventoryService {

    private static final int CHANGE_IN = 1;
    private static final int CHANGE_LOSS = 4;
    private static final int BATCH_STATUS_NORMAL = 1;
    private static final int BATCH_STATUS_EXPIRING = 2;
    private static final int BATCH_STATUS_EXPIRED = 3;
    private static final int BATCH_STATUS_SOLD_OUT = 4;
    private static final String REF_TYPE_BATCH = "BATCH";

    private final ProductMapper productMapper;
    private final ProductBatchMapper productBatchMapper;
    private final InventoryLogMapper inventoryLogMapper;

    @Override
    public PageResult<AdminBatchVO> batches(AdminBatchQuery query) {
        String batchNoFilter = query.getBatchNo() != null && !query.getBatchNo().isBlank()
                ? query.getBatchNo().trim() : null;
        Page<ProductBatch> page = productBatchMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                Wrappers.<ProductBatch>lambdaQuery()
                        .eq(query.getProductId() != null,
                                ProductBatch::getProductId, query.getProductId())
                        .eq(query.getStatus() != null,
                                ProductBatch::getStatus, query.getStatus())
                        .like(batchNoFilter != null,
                                ProductBatch::getBatchNo, batchNoFilter)
                        .orderByAsc(ProductBatch::getExpireDate)
                        .orderByAsc(ProductBatch::getId));
        return PageResult.of(toBatchVos(page.getRecords()),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<AdminBatchVO> expireWarning(int days) {
        int safeDays = Math.max(0, Math.min(days, 30));
        LocalDate deadline = LocalDate.now().plusDays(safeDays);
        return toBatchVos(productBatchMapper.selectList(
                Wrappers.<ProductBatch>lambdaQuery()
                        .gt(ProductBatch::getStock, 0)
                        .le(ProductBatch::getExpireDate, deadline)
                        .orderByAsc(ProductBatch::getExpireDate)
                        .orderByAsc(ProductBatch::getId)
                        .last("LIMIT 500")));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminBatchVO createBatch(AdminBatchSaveRequest request, Long adminId) {
        requireProduct(request.productId());
        if (request.lockStock() > request.stock()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "锁定库存不能超过批次库存");
        }
        if (request.productionDate().isAfter(request.expireDate())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(),
                    "生产日期不能晚于到期日期");
        }
        ProductBatch existing = productBatchMapper.selectOne(
                Wrappers.<ProductBatch>lambdaQuery()
                        .eq(ProductBatch::getProductId, request.productId())
                        .eq(ProductBatch::getBatchNo, request.batchNo().trim()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(),
                    "该商品下批次号已存在");
        }
        ProductBatch batch = new ProductBatch();
        batch.setProductId(request.productId());
        batch.setBatchNo(request.batchNo().trim());
        batch.setProductionDate(request.productionDate());
        batch.setExpireDate(request.expireDate());
        batch.setStock(request.stock());
        batch.setLockStock(request.lockStock());
        batch.setStatus(resolveBatchStatus(request.expireDate(), request.stock()));
        batch.setRemark(request.remark());
        productBatchMapper.insert(batch);
        if (request.stock() > 0) {
            productMapper.restoreStock(request.productId(), request.stock());
        }
        insertLog(batch, CHANGE_IN, request.stock(), 0, request.stock(),
                batch.getBatchNo(), adminId, "批次录入入库");
        return toBatchVo(batch);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminBatchVO reportLoss(Long batchId, AdminBatchLossRequest request, Long adminId) {
        ProductBatch batch = productBatchMapper.selectOne(
                Wrappers.<ProductBatch>lambdaQuery()
                        .eq(ProductBatch::getId, batchId));
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "商品批次不存在");
        }
        int quantity = request.quantity();
        int batchBefore = batch.getStock() == null ? 0 : batch.getStock();
        if (batchBefore < quantity) {
            throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT.getCode(),
                    "批次库存不足，无法报损");
        }
        Product product = productMapper.selectOne(
                Wrappers.<Product>lambdaQuery()
                        .eq(Product::getId, batch.getProductId())
                        .last("FOR UPDATE"));
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND.getCode(),
                    ErrorCode.PRODUCT_NOT_FOUND.getMessage());
        }
        int productBefore = product.getStock() == null ? 0 : product.getStock();
        if (productBefore < quantity
                || productMapper.deductStockForInventory(product.getId(), quantity) != 1) {
            throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT.getCode(),
                    "商品总库存不足，无法报损");
        }
        int batchAfter = batchBefore - quantity;
        if (productBatchMapper.reportLoss(batch.getId(), quantity,
                resolveBatchStatus(batch.getExpireDate(), batchAfter)) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(),
                    "批次库存已变化，请刷新后重试");
        }
        insertLog(batch, CHANGE_LOSS, quantity, batchBefore, batchAfter,
                "LOSS-" + batch.getId(), adminId, request.remark().trim());
        batch.setStock(batchAfter);
        batch.setStatus(resolveBatchStatus(batch.getExpireDate(), batchAfter));
        return toBatchVo(batch);
    }

    @Override
    public PageResult<AdminInventoryLogVO> logs(InventoryLogQuery query) {
        Page<InventoryLog> page = inventoryLogMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                Wrappers.<InventoryLog>lambdaQuery()
                        .eq(query.getProductId() != null,
                                InventoryLog::getProductId, query.getProductId())
                        .eq(query.getBatchId() != null,
                                InventoryLog::getBatchId, query.getBatchId())
                        .eq(query.getChangeType() != null,
                                InventoryLog::getChangeType, query.getChangeType())
                        .ge(query.getStartTime() != null,
                                InventoryLog::getCreateTime, query.getStartTime())
                        .le(query.getEndTime() != null,
                                InventoryLog::getCreateTime, query.getEndTime())
                        .orderByDesc(InventoryLog::getCreateTime)
                        .orderByDesc(InventoryLog::getId));
        return PageResult.of(toLogVos(page.getRecords()),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    private List<AdminBatchVO> toBatchVos(List<ProductBatch> batches) {
        if (batches.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = batches.stream()
                .map(ProductBatch::getProductId)
                .distinct()
                .toList();
        Map<Long, String> productNames = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));
        return batches.stream()
                .map(batch -> toBatchVo(batch, productNames))
                .toList();
    }

    private AdminBatchVO toBatchVo(ProductBatch batch) {
        Product product = productMapper.selectById(batch.getProductId());
        return toBatchVo(batch, product == null ? Map.of()
                : Map.of(product.getId(), product.getName()));
    }

    private AdminBatchVO toBatchVo(ProductBatch batch, Map<Long, String> productNames) {
        return new AdminBatchVO(
                batch.getId(),
                batch.getProductId(),
                productNames.get(batch.getProductId()),
                batch.getBatchNo(),
                batch.getProductionDate(),
                batch.getExpireDate(),
                batch.getStock(),
                batch.getLockStock(),
                batch.getStatus(),
                batchStatusText(batch.getStatus()),
                batch.getRemark(),
                batch.getCreateTime(),
                batch.getUpdateTime()
        );
    }

    private List<AdminInventoryLogVO> toLogVos(List<InventoryLog> logs) {
        if (logs.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = logs.stream()
                .map(InventoryLog::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> batchIds = logs.stream()
                .map(InventoryLog::getBatchId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> productNames = productIds.isEmpty() ? Map.of()
                : productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));
        Map<Long, String> batchNames = batchIds.isEmpty() ? Map.of()
                : productBatchMapper.selectBatchIds(batchIds).stream()
                .collect(Collectors.toMap(ProductBatch::getId, ProductBatch::getBatchNo));
        return logs.stream()
                .map(log -> new AdminInventoryLogVO(
                        log.getId(),
                        log.getProductId(),
                        productNames.get(log.getProductId()),
                        log.getBatchId(),
                        log.getBatchId() == null ? null : batchNames.get(log.getBatchId()),
                        log.getChangeType(),
                        changeTypeText(log.getChangeType()),
                        log.getChangeNum(),
                        log.getBeforeStock(),
                        log.getAfterStock(),
                        log.getRefType(),
                        log.getRefNo(),
                        log.getOperatorId(),
                        log.getRemark(),
                        log.getCreateTime()
                ))
                .toList();
    }

    private void requireProduct(Long productId) {
        if (productMapper.selectById(productId) == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND.getCode(),
                    ErrorCode.PRODUCT_NOT_FOUND.getMessage());
        }
    }

    private void insertLog(ProductBatch batch, int changeType, int changeNum,
            int beforeStock, int afterStock, String refNo, Long adminId, String remark) {
        InventoryLog log = new InventoryLog();
        log.setProductId(batch.getProductId());
        log.setBatchId(batch.getId());
        log.setChangeType(changeType);
        log.setChangeNum(changeNum);
        log.setBeforeStock(beforeStock);
        log.setAfterStock(afterStock);
        log.setRefType(REF_TYPE_BATCH);
        log.setRefNo(refNo);
        log.setOperatorId(adminId);
        log.setRemark(remark);
        inventoryLogMapper.insert(log);
    }

    private int resolveBatchStatus(LocalDate expireDate, int stock) {
        if (stock <= 0) {
            return BATCH_STATUS_SOLD_OUT;
        }
        LocalDate today = LocalDate.now();
        if (expireDate.isBefore(today)) {
            return BATCH_STATUS_EXPIRED;
        }
        if (!expireDate.isAfter(today.plusDays(3))) {
            return BATCH_STATUS_EXPIRING;
        }
        return BATCH_STATUS_NORMAL;
    }

    private String batchStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case BATCH_STATUS_NORMAL -> "正常";
            case BATCH_STATUS_EXPIRING -> "临期";
            case BATCH_STATUS_EXPIRED -> "过期";
            case BATCH_STATUS_SOLD_OUT -> "售罄";
            default -> "未知";
        };
    }

    private String changeTypeText(Integer changeType) {
        if (changeType == null) {
            return "未知";
        }
        return switch (changeType) {
            case 1 -> "入库";
            case 2 -> "出库";
            case 3 -> "退货";
            case CHANGE_LOSS -> "报损";
            case 5 -> "盘点";
            case 6 -> "取消回补";
            default -> "未知";
        };
    }
}
