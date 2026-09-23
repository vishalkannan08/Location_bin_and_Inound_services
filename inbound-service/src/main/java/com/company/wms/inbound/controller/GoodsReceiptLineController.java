package com.company.wms.inbound.controller;

import com.company.wms.inbound.dto.GoodsReceiptLineResponse;
import com.company.wms.inbound.dto.QuantityRequest;
import com.company.wms.inbound.service.GoodsReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * The seam putaway-service consumes. Kept separate from GoodsReceiptController
 * because these are machine-to-machine calls with a different audience and a
 * different change cadence.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Receipt Lines", description = "Line-level API consumed by putaway-service")
public class GoodsReceiptLineController {

    private final GoodsReceiptService goodsReceiptService;

    public GoodsReceiptLineController(GoodsReceiptService goodsReceiptService) {
        this.goodsReceiptService = goodsReceiptService;
    }

    @GetMapping("/goods-receipt-lines/{lineId}")
    @Operation(summary = "Get one line with its remaining put-away quantity")
    public GoodsReceiptLineResponse getLine(@PathVariable UUID lineId) {
        return goodsReceiptService.getLine(lineId);
    }

    @GetMapping("/warehouses/{warehouseId}/goods-receipt-lines/awaiting-put-away")
    @Operation(summary = "The put-away work queue: lines with stock still on the dock")
    public List<GoodsReceiptLineResponse> awaitingPutAway(@PathVariable UUID warehouseId) {
        return goodsReceiptService.linesAwaitingPutAway(warehouseId);
    }

    /**
     * Called by putaway-service after a put-away task completes. Rejects anything
     * that would take more than the line received (422 PUT_AWAY_EXCEEDS_RECEIVED),
     * and returns 409 if another worker allocated against the same line first.
     */
    @PostMapping("/goods-receipt-lines/{lineId}/put-away-allocation")
    @Operation(summary = "Record that a quantity from this line has been put away")
    public GoodsReceiptLineResponse allocateToPutAway(@PathVariable UUID lineId,
                                                      @Valid @RequestBody QuantityRequest request) {
        return goodsReceiptService.allocateToPutAway(lineId, request.quantity());
    }
}
