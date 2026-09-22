package com.company.wms.locationbin.dto;

import com.company.wms.locationbin.domain.LocationStatus;
import com.company.wms.locationbin.domain.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateLocationRequest(

        @NotNull(message = "warehouseId is required")
        UUID warehouseId,

        @NotBlank(message = "locationCode is required")
        @Size(max = 50, message = "locationCode must be at most 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9\\-_/]*$",
                message = "locationCode may contain letters, digits, hyphen, underscore and slash only")
        String locationCode,

        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must be at most 150 characters")
        String name,

        @NotNull(message = "type is required")
        LocationType type,

        /** Optional. Defaults to ACTIVE when omitted. */
        LocationStatus status
) {
    public LocationStatus statusOrDefault() {
        return status != null ? status : LocationStatus.ACTIVE;
    }

    /** Codes are case-insensitive business keys; store them normalised. */
    public String normalisedCode() {
        return locationCode == null ? null : locationCode.trim().toUpperCase();
    }
}
