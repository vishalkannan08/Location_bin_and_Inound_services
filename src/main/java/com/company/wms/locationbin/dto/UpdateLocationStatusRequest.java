package com.company.wms.locationbin.dto;

import com.company.wms.locationbin.domain.LocationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateLocationStatusRequest(

        @NotNull(message = "status is required")
        LocationStatus status
) {
}
