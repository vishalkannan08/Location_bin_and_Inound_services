package com.company.wms.locationbin.mapper;

import com.company.wms.locationbin.domain.Bin;
import com.company.wms.locationbin.dto.BinResponse;

public final class BinMapper {

    private BinMapper() {
    }

    /**
     * Touches bin.getLocation(), so callers must be inside the transaction or
     * must have loaded the bin with its location eagerly.
     */
    public static BinResponse toResponse(Bin bin) {
        return new BinResponse(
                bin.getId(),
                bin.getLocation().getId(),
                bin.getLocation().getLocationCode(),
                bin.getLocation().getWarehouseId(),
                bin.getBinCode(),
                bin.getCapacity(),
                bin.getStatus(),
                bin.acceptsPutAway(),
                bin.getCreatedAt(),
                bin.getUpdatedAt()
        );
    }
}
