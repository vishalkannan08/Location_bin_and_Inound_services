package com.company.wms.locationbin.dto;

import com.company.wms.locationbin.domain.BinStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * warehouseId and acceptsPutAway are denormalised onto the response on purpose:
 * putaway-service needs both to validate a target bin in a single call, and
 * must not reach into this service's database to get them.
 */
public record BinResponse(
        UUID id,
        UUID locationId,
        String locationCode,
        UUID warehouseId,
        String binCode,
        Integer capacity,
        BinStatus status,
        boolean acceptsPutAway,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
