package com.company.wms.inbound.dto;

import com.company.wms.inbound.domain.GoodsReceiptStatus;
import com.company.wms.inbound.domain.ReferenceType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record GoodsReceiptResponse(
        UUID id,
        String receiptNumber,
        UUID warehouseId,
        ReferenceType referenceType,
        String referenceId,
        GoodsReceiptStatus status,
        boolean hasDiscrepancy,
        OffsetDateTime receivedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<GoodsReceiptLineResponse> lines
) {
}
