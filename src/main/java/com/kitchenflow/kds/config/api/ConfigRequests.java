package com.kitchenflow.kds.config.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ConfigRequests {
    private ConfigRequests() {}

    public record StationPatch(@Size(max = 80) String name, Integer displayOrder, Boolean active) {}
    public record StationCreateRequest(@NotBlank @Size(max = 80) String name, @Min(0) int displayOrder) {}
    public record SettingsRequest(@NotBlank @Size(max = 120) String outletName, @Size(max = 15) String gstin,
            @Min(1) int amberPercent, @Min(2) int redPercent) {}
}
