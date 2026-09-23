package com.company.wms.inbound.dto;

import com.company.wms.inbound.domain.ReferenceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateGoodsReceiptRequest(

        @NotNull(message = "warehouseId is required")
        UUID warehouseId,

        @NotNull(message = "referenceType is required")
        ReferenceType referenceType,

        @NotBlank(message = "referenceId is required")
        @Size(max = 100, message = "referenceId must be at most 100 characters")
        String referenceId,

        /**
         * A receipt with no lines is meaningless - it records that nothing arrived.
         * {@code @Valid} on the list is what makes the nested line validation run.
         */
        @NotEmpty(message = "at least one line is required")
        @Size(max = 500, message = "a receipt may not have more than 500 lines")
        @Valid
        List<CreateGoodsReceiptLineRequest> lines
) {
}
