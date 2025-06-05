package com.cloudsec.compliance.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PaginationResult<T>(
    @NotNull(message = "Items list cannot be null")
    @Valid
    List<T> items,
    
    String nextPageToken,
    
    @NotNull(message = "HasMore flag cannot be null")
    Boolean hasMore
) {
    public PaginationResult {
        if (items == null) items = List.of();
        if (hasMore == null) hasMore = false;
    }
}
