package com.company.wms.locationbin.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Standard error body for every non-2xx response in the service (DEV-01).
 * Keep this shape identical across all WMS services.
 */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String correlationId,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, Object rejectedValue, String message) {
    }
}
