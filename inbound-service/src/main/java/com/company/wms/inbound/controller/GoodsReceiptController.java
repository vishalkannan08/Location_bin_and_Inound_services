package com.company.wms.inbound.controller;

import com.company.wms.inbound.domain.GoodsReceiptStatus;
import com.company.wms.inbound.dto.CreateGoodsReceiptRequest;
import com.company.wms.inbound.dto.GoodsReceiptResponse;
import com.company.wms.inbound.dto.PageResponse;
import com.company.wms.inbound.dto.QuantityRequest;
import com.company.wms.inbound.service.GoodsReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Goods Receipts", description = "Inbound goods receipts and their lines")
public class GoodsReceiptController {

    private final GoodsReceiptService goodsReceiptService;

    public GoodsReceiptController(GoodsReceiptService goodsReceiptService) {
        this.goodsReceiptService = goodsReceiptService;
    }

    @PostMapping("/goods-receipts")
    @Operation(summary = "Create a goods receipt with one or more SKU lines")
    public ResponseEntity<GoodsReceiptResponse> create(@Valid @RequestBody CreateGoodsReceiptRequest request,
                                                       UriComponentsBuilder uriBuilder) {
        GoodsReceiptResponse response = goodsReceiptService.create(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/goods-receipts/{id}").build(response.id()))
                .body(response);
    }

    @GetMapping("/goods-receipts/{id}")
    @Operation(summary = "Get a goods receipt with its lines")
    public GoodsReceiptResponse getById(@PathVariable UUID id) {
        return goodsReceiptService.getById(id);
    }

    @GetMapping("/goods-receipts/by-number/{receiptNumber}")
    @Operation(summary = "Get a goods receipt by its human-readable number, e.g. GR-000001")
    public GoodsReceiptResponse getByNumber(@PathVariable String receiptNumber) {
        return goodsReceiptService.getByReceiptNumber(receiptNumber);
    }

    @GetMapping("/warehouses/{warehouseId}/goods-receipts")
    @Operation(summary = "List a warehouse's goods receipts, optionally filtered by status")
    public PageResponse<GoodsReceiptResponse> listByWarehouse(
            @PathVariable UUID warehouseId,
            @RequestParam(required = false) GoodsReceiptStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return goodsReceiptService.listByWarehouse(warehouseId, status, pageable);
    }

    @PostMapping("/goods-receipts/{id}/lines/{lineId}/receive")
    @Operation(summary = "Record the physical count for a line")
    public GoodsReceiptResponse receiveLine(@PathVariable UUID id,
                                            @PathVariable UUID lineId,
                                            @Valid @RequestBody QuantityRequest request) {
        return goodsReceiptService.receiveLine(id, lineId, request.quantity());
    }

    @PostMapping("/goods-receipts/{id}/lines/{lineId}/cancel")
    @Operation(summary = "Cancel a line that will not be received")
    public GoodsReceiptResponse cancelLine(@PathVariable UUID id, @PathVariable UUID lineId) {
        return goodsReceiptService.cancelLine(id, lineId);
    }

    @PostMapping("/goods-receipts/{id}/cancel")
    @Operation(summary = "Cancel the whole receipt (only while nothing has been put away)")
    public GoodsReceiptResponse cancel(@PathVariable UUID id) {
        return goodsReceiptService.cancel(id);
    }
}
