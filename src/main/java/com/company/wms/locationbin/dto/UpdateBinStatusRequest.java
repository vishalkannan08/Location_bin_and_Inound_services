package com.company.wms.locationbin.dto;

import com.company.wms.locationbin.domain.BinStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateBinStatusRequest(

        @NotNull(message = "status is required")
        BinStatus status
) {
}
