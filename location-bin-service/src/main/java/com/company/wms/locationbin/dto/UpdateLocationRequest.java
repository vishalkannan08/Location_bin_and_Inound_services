package com.company.wms.locationbin.dto;

import com.company.wms.locationbin.domain.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * locationCode and warehouseId are immutable business keys and are not updatable.
 */
public record UpdateLocationRequest(

        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must be at most 150 characters")
        String name,

        @NotNull(message = "type is required")
        LocationType type
) {
}
