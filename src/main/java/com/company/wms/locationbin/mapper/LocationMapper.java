package com.company.wms.locationbin.mapper;

import com.company.wms.locationbin.domain.Location;
import com.company.wms.locationbin.dto.LocationResponse;

public final class LocationMapper {

    private LocationMapper() {
    }

    public static LocationResponse toResponse(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getWarehouseId(),
                location.getLocationCode(),
                location.getName(),
                location.getType(),
                location.getStatus(),
                location.getCreatedAt(),
                location.getUpdatedAt()
        );
    }
}
