package com.company.wms.inbound.mapper;

import com.company.wms.inbound.domain.GoodsReceipt;
import com.company.wms.inbound.domain.GoodsReceiptLine;
import com.company.wms.inbound.domain.GoodsReceiptLineStatus;
import com.company.wms.inbound.dto.GoodsReceiptLineResponse;
import com.company.wms.inbound.dto.GoodsReceiptResponse;

public final class GoodsReceiptMapper {

    private GoodsReceiptMapper() {
    }

    /** Must be called inside the transaction - it walks the lines collection. */
    public static GoodsReceiptResponse toResponse(GoodsReceipt receipt) {
        return new GoodsReceiptResponse(
                receipt.getId(),
                receipt.getReceiptNumber(),
                receipt.getWarehouseId(),
                receipt.getReferenceType(),
                receipt.getReferenceId(),
                receipt.getStatus(),
                receipt.hasDiscrepancy(),
                receipt.getReceivedAt(),
                receipt.getCreatedAt(),
                receipt.getUpdatedAt(),
                receipt.getLines().stream().map(GoodsReceiptMapper::toLineResponse).toList()
        );
    }

    public static GoodsReceiptLineResponse toLineResponse(GoodsReceiptLine line) {
        GoodsReceipt receipt = line.getGoodsReceipt();
        return new GoodsReceiptLineResponse(
                line.getId(),
                receipt.getId(),
                receipt.getReceiptNumber(),
                receipt.getWarehouseId(),
                line.getLineNumber(),
                line.getSkuId(),
                line.getExpectedQuantity(),
                line.getReceivedQuantity(),
                line.getPutAwayQuantity(),
                line.remainingQuantity(),
                isEligibleForPutAway(line),
                line.hasDiscrepancy(),
                line.getUom(),
                line.getBatchNumber(),
                line.getSerialNumber(),
                line.getStatus(),
                line.getCreatedAt(),
                line.getUpdatedAt()
        );
    }

    /**
     * A line may be put away when it is not cancelled and stock is still sitting
     * on the dock. Single definition of the rule, used by the API and by
     * putaway-service through it.
     */
    private static boolean isEligibleForPutAway(GoodsReceiptLine line) {
        return line.getStatus() != GoodsReceiptLineStatus.CANCELLED
                && line.remainingQuantity().signum() > 0;
    }
}
