package com.npst.loggingapi.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * A stable page shape for the search APIs.
 *
 * <p>Spring's Page is deliberately not serialized directly - its JSON structure
 * is an implementation detail that has changed between Spring versions, and a
 * support tool or a NestJS client reading these endpoints should not break when
 * the framework is upgraded.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext());
    }
}
