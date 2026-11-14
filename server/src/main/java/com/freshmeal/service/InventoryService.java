package com.freshmeal.service;

import com.freshmeal.entity.OrderItem;
import java.util.List;

public interface InventoryService {

    void deductForOrder(List<OrderItem> items, String orderNo, Long operatorId);

    void restoreForOrder(String orderNo, Long operatorId, String remark);
}
