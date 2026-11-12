package com.kitchenflow.kds.menu.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public final class MenuAdminRequests {
    private MenuAdminRequests() {}

    public record CategoryRequest(@Size(max = 60) String name,
            @DecimalMin("0.00") BigDecimal gstRate, Boolean active) {}
    public record ModifierGroupRequest(@Size(max = 60) String name,
            String selectionType, Integer minSelect, Integer maxSelect) {}
    public record ModifierRequest(@Size(max = 60) String name, BigDecimal priceDelta, Boolean active) {}
    public record ItemStationsRequest(@NotEmpty List<Long> stationIds, Long expectedVersion) {}
}
