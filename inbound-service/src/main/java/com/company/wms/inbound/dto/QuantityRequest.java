package com.company.wms.inbound.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Body for both "record the received quantity" and "record a put-away
 * allocation". Same shape, same validation, so one record serves both.
 */
public record QuantityRequest(

        @NotNull(message = "quantity is required")
        @DecimalMin(value = "0", message = "quantity cannot be negative")
        @DecimalMax(value = "999999999999", message = "quantity is implausibly large")
        @Digits(integer = 12, fraction = 3, message = "quantity allows at most 3 decimal places")
        BigDecimal quantity
) {
}
