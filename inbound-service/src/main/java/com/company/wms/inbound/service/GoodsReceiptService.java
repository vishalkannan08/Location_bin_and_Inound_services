package com.company.wms.inbound.service;

import com.company.wms.inbound.domain.GoodsReceipt;
import com.company.wms.inbound.domain.GoodsReceiptLine;
import com.company.wms.inbound.domain.GoodsReceiptLineStatus;
import com.company.wms.inbound.domain.GoodsReceiptStatus;
import com.company.wms.inbound.dto.CreateGoodsReceiptLineRequest;
import com.company.wms.inbound.dto.CreateGoodsReceiptRequest;
import com.company.wms.inbound.dto.GoodsReceiptLineResponse;
import com.company.wms.inbound.dto.GoodsReceiptResponse;
import com.company.wms.inbound.dto.PageResponse;
import com.company.wms.inbound.exception.ResourceNotFoundException;
import com.company.wms.inbound.mapper.GoodsReceiptMapper;
import com.company.wms.inbound.repository.GoodsReceiptLineRepository;
import com.company.wms.inbound.repository.GoodsReceiptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class GoodsReceiptService {

    private static final Logger log = LoggerFactory.getLogger(GoodsReceiptService.class);

    /** Lines that may still have stock waiting on the dock. */
    private static final List<GoodsReceiptLineStatus> AWAITING_PUT_AWAY = List.of(
            GoodsReceiptLineStatus.RECEIVED,
            GoodsReceiptLineStatus.PARTIALLY_PUT_AWAY);

    private final GoodsReceiptRepository receiptRepository;
    private final GoodsReceiptLineRepository lineRepository;

    public GoodsReceiptService(GoodsReceiptRepository receiptRepository,
                               GoodsReceiptLineRepository lineRepository) {
        this.receiptRepository = receiptRepository;
        this.lineRepository = lineRepository;
    }

    @Transactional
    public GoodsReceiptResponse create(CreateGoodsReceiptRequest request) {
        // NOTE: warehouseId is NOT validated against warehouse-service, and skuId
        // is not validated against any product master. Both services are out of
        // scope for this increment. Add HTTP clients here when they exist - never
        // a direct database read.
        GoodsReceipt receipt = new GoodsReceipt(
                nextReceiptNumber(),
                request.warehouseId(),
                request.referenceType(),
                request.referenceId().trim());

        for (CreateGoodsReceiptLineRequest line : request.lines()) {
            receipt.addLine(
                    line.normalisedSku(),
                    line.expectedQuantity(),
                    line.receivedOrZero(),
                    line.normalisedUom(),
                    trimToNull(line.batchNumber()),
                    trimToNull(line.serialNumber()));
        }

        GoodsReceipt saved = receiptRepository.save(receipt);
        log.info("Created goods receipt {} ({} lines, status {}) for warehouse {} against {} {}",
                saved.getReceiptNumber(), saved.getLines().size(), saved.getStatus(),
                saved.getWarehouseId(), saved.getReferenceType(), saved.getReferenceId());
        return GoodsReceiptMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public GoodsReceiptResponse getById(UUID id) {
        return GoodsReceiptMapper.toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public GoodsReceiptResponse getByReceiptNumber(String receiptNumber) {
        GoodsReceipt receipt = receiptRepository.findByReceiptNumber(receiptNumber.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RECEIPT_NOT_FOUND", "Goods receipt not found: " + receiptNumber));
        return GoodsReceiptMapper.toResponse(receipt);
    }

    @Transactional(readOnly = true)
    public PageResponse<GoodsReceiptResponse> listByWarehouse(UUID warehouseId,
                                                              GoodsReceiptStatus status,
                                                              Pageable pageable) {
        Page<GoodsReceipt> page = (status == null)
                ? receiptRepository.findByWarehouseId(warehouseId, pageable)
                : receiptRepository.findByWarehouseIdAndStatus(warehouseId, status, pageable);
        return PageResponse.from(page, GoodsReceiptMapper::toResponse);
    }

    /** Record the physical count for one line. */
    @Transactional
    public GoodsReceiptResponse receiveLine(UUID receiptId, UUID lineId, BigDecimal quantity) {
        GoodsReceipt receipt = findOrThrow(receiptId);
        receipt.receiveLine(lineId, quantity);
        log.info("Receipt {} line {} received quantity {}", receipt.getReceiptNumber(), lineId, quantity);
        return GoodsReceiptMapper.toResponse(receipt);
    }

    @Transactional
    public GoodsReceiptResponse cancelLine(UUID receiptId, UUID lineId) {
        GoodsReceipt receipt = findOrThrow(receiptId);
        receipt.cancelLine(lineId);
        return GoodsReceiptMapper.toResponse(receipt);
    }

    @Transactional
    public GoodsReceiptResponse cancel(UUID receiptId) {
        GoodsReceipt receipt = findOrThrow(receiptId);
        receipt.cancel();
        log.info("Receipt {} cancelled", receipt.getReceiptNumber());
        return GoodsReceiptMapper.toResponse(receipt);
    }

    // --- line-level API, used by putaway-service --------------------------

    @Transactional(readOnly = true)
    public GoodsReceiptLineResponse getLine(UUID lineId) {
        return GoodsReceiptMapper.toLineResponse(findLineOrThrow(lineId));
    }

    /**
     * Called when a put-away task completes. Goes through the aggregate root so
     * the receipt's status is recalculated in the same transaction - otherwise
     * a receipt would stay RECEIVED forever after its last line was put away.
     */
    @Transactional
    public GoodsReceiptLineResponse allocateToPutAway(UUID lineId, BigDecimal quantity) {
        GoodsReceiptLine line = findLineOrThrow(lineId);
        GoodsReceipt receipt = line.getGoodsReceipt();
        receipt.allocateLineToPutAway(lineId, quantity);
        log.info("Receipt {} line {} allocated {} to put-away ({} remaining)",
                receipt.getReceiptNumber(), lineId, quantity, line.remainingQuantity());
        return GoodsReceiptMapper.toLineResponse(line);
    }

    /** The put-away work queue for a warehouse. */
    @Transactional(readOnly = true)
    public List<GoodsReceiptLineResponse> linesAwaitingPutAway(UUID warehouseId) {
        return lineRepository.findAwaitingPutAway(warehouseId, AWAITING_PUT_AWAY).stream()
                .map(GoodsReceiptMapper::toLineResponse)
                .toList();
    }

    // --- internals --------------------------------------------------------

    private GoodsReceipt findOrThrow(UUID id) {
        return receiptRepository.findByIdWithLines(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RECEIPT_NOT_FOUND", "Goods receipt not found: " + id));
    }

    private GoodsReceiptLine findLineOrThrow(UUID lineId) {
        return lineRepository.findByIdWithReceipt(lineId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RECEIPT_LINE_NOT_FOUND", "Goods receipt line not found: " + lineId));
    }

    private String nextReceiptNumber() {
        return "GR-%06d".formatted(receiptRepository.nextReceiptSequence());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
