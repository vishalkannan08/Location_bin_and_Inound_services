package com.company.wms.locationbin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * binCode and locationId are immutable. A bin cannot be moved to another location.
 */
public record UpdateBinRequest(

        @NotNull(message = "capacity is required")
        @Min(value = 1, message = "capacity must be at least 1")
        @Max(value = 1_000_000, message = "capacity must be at most 1000000")
        Integer capacity
) {
}
