package com.company.wms.inbound.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateGoodsReceiptLineRequest(

        @NotBlank(message = "skuId is required")
        @Size(max = 60, message = "skuId must be at most 60 characters")
        String skuId,

        @NotNull(message = "expectedQuantity is required")
        @DecimalMin(value = "0", message = "expectedQuantity cannot be negative")
        @DecimalMax(value = "999999999999", message = "expectedQuantity is implausibly large")
        @Digits(integer = 12, fraction = 3, message = "expectedQuantity allows at most 3 decimal places")
        BigDecimal expectedQuantity,

        /**
         * Optional. Omit it to create the line as PENDING and count the stock in
         * later with POST /lines/{id}/receive. The requirement doc's sample
         * payload supplies it at creation time, so both flows work.
         */
        @DecimalMin(value = "0", message = "receivedQuantity cannot be negative")
        @DecimalMax(value = "999999999999", message = "receivedQuantity is implausibly large")
        @Digits(integer = 12, fraction = 3, message = "receivedQuantity allows at most 3 decimal places")
        BigDecimal receivedQuantity,

        @NotBlank(message = "uom is required")
        @Size(max = 10, message = "uom must be at most 10 characters")
        String uom,

        @Size(max = 60, message = "batchNumber must be at most 60 characters")
        String batchNumber,

        @Size(max = 60, message = "serialNumber must be at most 60 characters")
        String serialNumber
) {
    public BigDecimal receivedOrZero() {
        return receivedQuantity != null ? receivedQuantity : BigDecimal.ZERO;
    }

    public String normalisedSku() {
        return skuId == null ? null : skuId.trim().toUpperCase();
    }

    public String normalisedUom() {
        return uom == null ? null : uom.trim().toUpperCase();
    }
}
