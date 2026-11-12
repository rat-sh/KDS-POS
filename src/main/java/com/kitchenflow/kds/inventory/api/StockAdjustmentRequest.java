package com.kitchenflow.kds.inventory.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockAdjustmentRequest(@NotNull Integer quantityDelta, @NotBlank @Size(max = 250) String reason,
        Long expectedVersion) {}
