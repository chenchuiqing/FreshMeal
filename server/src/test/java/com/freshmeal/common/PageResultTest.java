package com.freshmeal.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class PageResultTest {

    @Test
    void calculatesTotalPagesWithCeilingDivision() {
        PageResult<String> result = PageResult.of(List.of("item"), 101, 11, 10);

        assertThat(result.getPages()).isEqualTo(11);
        assertThat(result.getTotal()).isEqualTo(101);
        assertThat(result.getCurrent()).isEqualTo(11);
        assertThat(result.getSize()).isEqualTo(10);
    }

    @Test
    void emptyResultHasNoPages() {
        PageResult<String> result = PageResult.of(List.of(), 0, 1, 10);

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getPages()).isZero();
    }

    @Test
    void exactlyDivisibleTotalUsesExactPageCount() {
        List<Integer> records = IntStream.range(0, 10).boxed().toList();
        PageResult<Integer> result = PageResult.of(records, 100, 10, 10);

        assertThat(result.getPages()).isEqualTo(10);
        assertThat(result.getRecords()).hasSize(10);
    }
}
