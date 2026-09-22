package com.company.wms.locationbin.dto;

import com.company.wms.locationbin.domain.LocationStatus;
import com.company.wms.locationbin.domain.LocationType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LocationResponse(
        UUID id,
        UUID warehouseId,
        String locationCode,
        String name,
        LocationType type,
        LocationStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
