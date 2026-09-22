package com.company.wms.locationbin.dto;

import com.company.wms.locationbin.domain.BinStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateBinRequest(

        @NotNull(message = "locationId is required")
        UUID locationId,

        @NotBlank(message = "binCode is required")
        @Size(max = 50, message = "binCode must be at most 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9\\-_/]*$",
                message = "binCode may contain letters, digits, hyphen, underscore and slash only")
        String binCode,

        @NotNull(message = "capacity is required")
        @Min(value = 1, message = "capacity must be at least 1")
        @Max(value = 1_000_000, message = "capacity must be at most 1000000")
        Integer capacity,

        /** Optional. Defaults to AVAILABLE when omitted. */
        BinStatus status
) {
    public BinStatus statusOrDefault() {
        return status != null ? status : BinStatus.AVAILABLE;
    }

    public String normalisedCode() {
        return binCode == null ? null : binCode.trim().toUpperCase();
    }
}
