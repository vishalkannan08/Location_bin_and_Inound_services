package com.company.wms.inbound.dto;

import com.company.wms.inbound.domain.GoodsReceiptLineStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * {@code remainingQuantity} and {@code eligibleForPutAway} are computed here so
 * putaway-service does not have to re-derive the rule and get it wrong.
 */
public record GoodsReceiptLineResponse(
        UUID id,
        UUID goodsReceiptId,
        String receiptNumber,
        UUID warehouseId,
        Integer lineNumber,
        String skuId,
        BigDecimal expectedQuantity,
        BigDecimal receivedQuantity,
        BigDecimal putAwayQuantity,
        BigDecimal remainingQuantity,
        boolean eligibleForPutAway,
        boolean hasDiscrepancy,
        String uom,
        String batchNumber,
        String serialNumber,
        GoodsReceiptLineStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
