package com.freshmeal.dto.admin;

import com.freshmeal.dto.common.PageQuery;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryLogQuery extends PageQuery {

    private Long productId;
    private Long batchId;
    private Integer changeType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
