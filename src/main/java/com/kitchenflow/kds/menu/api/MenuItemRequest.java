package com.kitchenflow.kds.menu.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record MenuItemRequest(@NotBlank @Size(max = 120) String name, @NotBlank String category,
        @NotNull @DecimalMin("0.00") BigDecimal price, @NotEmpty List<String> stations,
        @Min(1) int prepSeconds, boolean available, @Min(0) int stock,
        boolean stockTracking, List<Long> modifierGroups) {}
