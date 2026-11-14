package com.freshmeal.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.freshmeal.common.BusinessException;
import com.freshmeal.entity.InventoryLog;
import com.freshmeal.entity.OrderItem;
import com.freshmeal.entity.Product;
import com.freshmeal.entity.ProductBatch;
import com.freshmeal.enums.ErrorCode;
import com.freshmeal.mapper.InventoryLogMapper;
import com.freshmeal.mapper.ProductBatchMapper;
import com.freshmeal.mapper.ProductMapper;
import com.freshmeal.service.InventoryService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final int CHANGE_OUT = 2;
    private static final int CHANGE_CANCEL_RESTORE = 6;
    private static final String REF_TYPE_ORDER = "ORDER";

    private final ProductMapper productMapper;
    private final ProductBatchMapper productBatchMapper;
    private final InventoryLogMapper inventoryLogMapper;

    @Override
    public void deductForOrder(List<OrderItem> items, String orderNo, Long operatorId) {
        Map<Long, Integer> quantityByProduct = new LinkedHashMap<>();
        for (OrderItem item : items) {
            quantityByProduct.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        for (Map.Entry<Long, Integer> entry : quantityByProduct.entrySet()) {
            Product product = productMapper.selectById(entry.getKey());
            if (product == null || product.getStatus() == null || product.getStatus() != 1) {
                throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE.getCode(),
                        ErrorCode.PRODUCT_UNAVAILABLE.getMessage());
            }
            int quantity = entry.getValue();
            Integer beforeProductStock = product.getStock();
            if (beforeProductStock == null || beforeProductStock < quantity
                    || productMapper.deductStockOptimistically(product.getId(), quantity) != 1) {
                throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT.getCode(),
                        ErrorCode.INVENTORY_INSUFFICIENT.getMessage());
            }
            insertProductLog(product.getId(), quantity, beforeProductStock,
                    beforeProductStock - quantity, CHANGE_OUT, orderNo, operatorId,
                    "下单扣减商品库存");
            deductBatches(product.getId(), quantity, orderNo, operatorId);
        }
    }

    @Override
    public void restoreForOrder(String orderNo, Long operatorId, String remark) {
        List<InventoryLog> sourceLogs = inventoryLogMapper.selectList(
                Wrappers.<InventoryLog>lambdaQuery()
                        .eq(InventoryLog::getRefNo, orderNo)
                        .eq(InventoryLog::getChangeType, CHANGE_OUT));

        sourceLogs.stream()
                .filter(log -> log.getBatchId() == null)
                .forEach(log -> restoreProduct(log, orderNo, operatorId, remark));
        sourceLogs.stream()
                .filter(log -> log.getBatchId() != null)
                .forEach(log -> restoreBatch(log, orderNo, operatorId, remark));
    }

    private void deductBatches(Long productId, int quantity, String orderNo, Long operatorId) {
        int remaining = quantity;
        List<ProductBatch> batches = productBatchMapper.selectAvailableFefo(productId);
        for (ProductBatch batch : batches) {
            if (remaining == 0) {
                break;
            }
            int deducted = Math.min(remaining, batch.getStock());
            if (productBatchMapper.deductStock(batch.getId(), deducted) != 1) {
                continue;
            }
            insertBatchLog(batch, deducted, CHANGE_OUT, orderNo, operatorId,
                    "下单按FEFO扣减批次库存");
            remaining -= deducted;
        }
        if (remaining > 0) {
            throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT.getCode(),
                    "可用批次库存不足，无法履约");
        }
    }

    private void restoreProduct(InventoryLog source, String orderNo, Long operatorId, String remark) {
        Product product = productMapper.selectById(source.getProductId());
        if (product == null) {
            return;
        }
        int before = product.getStock() == null ? 0 : product.getStock();
        if (productMapper.restoreStock(source.getProductId(), source.getChangeNum()) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(),
                    ErrorCode.INTERNAL_ERROR.getMessage());
        }
        insertProductLog(source.getProductId(), source.getChangeNum(), before,
                before + source.getChangeNum(), CHANGE_CANCEL_RESTORE, orderNo,
                operatorId, remark);
    }

    private void restoreBatch(InventoryLog source, String orderNo, Long operatorId, String remark) {
        ProductBatch batch = productBatchMapper.selectById(source.getBatchId());
        if (batch == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(),
                    ErrorCode.INTERNAL_ERROR.getMessage());
        }
        int before = batch.getStock() == null ? 0 : batch.getStock();
        if (productBatchMapper.restoreStock(batch.getId(), source.getChangeNum()) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(),
                    ErrorCode.INTERNAL_ERROR.getMessage());
        }
        InventoryLog log = new InventoryLog();
        log.setProductId(batch.getProductId());
        log.setBatchId(batch.getId());
        log.setChangeType(CHANGE_CANCEL_RESTORE);
        log.setChangeNum(source.getChangeNum());
        log.setBeforeStock(before);
        log.setAfterStock(before + source.getChangeNum());
        log.setRefType(REF_TYPE_ORDER);
        log.setRefNo(orderNo);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        inventoryLogMapper.insert(log);
    }

    private void insertProductLog(Long productId, int quantity, int before, int after,
            int changeType, String orderNo, Long operatorId, String remark) {
        InventoryLog log = new InventoryLog();
        log.setProductId(productId);
        log.setBatchId(null);
        log.setChangeType(changeType);
        log.setChangeNum(quantity);
        log.setBeforeStock(before);
        log.setAfterStock(after);
        log.setRefType(REF_TYPE_ORDER);
        log.setRefNo(orderNo);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        inventoryLogMapper.insert(log);
    }

    private void insertBatchLog(ProductBatch batch, int quantity, int changeType,
            String orderNo, Long operatorId, String remark) {
        ProductBatch updated = productBatchMapper.selectById(batch.getId());
        if (updated == null || Objects.isNull(updated.getStock())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(),
                    ErrorCode.INTERNAL_ERROR.getMessage());
        }
        InventoryLog log = new InventoryLog();
        log.setProductId(batch.getProductId());
        log.setBatchId(batch.getId());
        log.setChangeType(changeType);
        log.setChangeNum(quantity);
        log.setBeforeStock(batch.getStock());
        log.setAfterStock(updated.getStock());
        log.setRefType(REF_TYPE_ORDER);
        log.setRefNo(orderNo);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        inventoryLogMapper.insert(log);
    }
}
