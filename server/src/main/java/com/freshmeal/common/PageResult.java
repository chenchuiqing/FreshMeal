package com.freshmeal.common;

import java.util.List;

public final class PageResult<T> {

    private final List<T> records;
    private final long total;
    private final long current;
    private final long size;
    private final long pages;

    private PageResult(List<T> records, long total, long current, long size, long pages) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
        this.pages = pages;
    }

    public static <T> PageResult<T> of(List<T> records, long total, long current, long size) {
        List<T> safeRecords = records == null ? List.of() : records;
        long safeTotal = Math.max(total, 0);
        long pageSize = Math.max(size, 0);
        long pageCount = safeTotal == 0 || pageSize == 0
                ? 0
                : Math.floorDiv(safeTotal - 1, pageSize) + 1;
        return new PageResult<>(safeRecords, safeTotal, current, pageSize, pageCount);
    }

    public List<T> getRecords() {
        return records;
    }

    public long getTotal() {
        return total;
    }

    public long getCurrent() {
        return current;
    }

    public long getSize() {
        return size;
    }

    public long getPages() {
        return pages;
    }
}
