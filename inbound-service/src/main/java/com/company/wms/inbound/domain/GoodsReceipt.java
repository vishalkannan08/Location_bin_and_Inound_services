package com.company.wms.inbound.domain;

import com.company.wms.inbound.exception.BusinessRuleException;
import com.company.wms.inbound.exception.ResourceNotFoundException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root. Lines are only ever created and changed through this class,
 * so the receipt's status can never drift out of step with its lines.
 */
@Entity
@Table(name = "goods_receipt")
public class GoodsReceipt {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "receipt_number", nullable = false, length = 30, updatable = false)
    private String receiptNumber;

    /** Logical reference to warehouse-service. No foreign key. */
    @Column(name = "warehouse_id", nullable = false, updatable = false)
    private UUID warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 30, updatable = false)
    private ReferenceType referenceType;

    /** The PO number, transfer number, RMA number and so on. Free text by design. */
    @Column(name = "reference_id", nullable = false, length = 100, updatable = false)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private GoodsReceiptStatus status;

    @Column(name = "received_at")
    private OffsetDateTime receivedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    private List<GoodsReceiptLine> lines = new ArrayList<>();

    protected GoodsReceipt() {
        // required by JPA
    }

    public GoodsReceipt(String receiptNumber, UUID warehouseId,
                        ReferenceType referenceType, String referenceId) {
        this.id = UUID.randomUUID();
        this.receiptNumber = receiptNumber;
        this.warehouseId = warehouseId;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.status = GoodsReceiptStatus.DRAFT;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // --- behaviour -------------------------------------------------------

    public GoodsReceiptLine addLine(String skuId, BigDecimal expectedQuantity,
                                    BigDecimal receivedQuantity, String uom,
                                    String batchNumber, String serialNumber) {
        if (status.isTerminal()) {
            throw new BusinessRuleException("RECEIPT_" + status,
                    "Lines cannot be added to a %s receipt".formatted(status));
        }
        int nextLineNumber = lines.stream()
                .mapToInt(GoodsReceiptLine::getLineNumber)
                .max()
                .orElse(0) + 1;

        GoodsReceiptLine line = new GoodsReceiptLine(this, nextLineNumber, skuId,
                expectedQuantity, receivedQuantity, uom, batchNumber, serialNumber);
        lines.add(line);
        recalculateStatus();
        return line;
    }

    public GoodsReceiptLine lineById(UUID lineId) {
        return lines.stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("RECEIPT_LINE_NOT_FOUND",
                        "Line %s is not part of receipt %s".formatted(lineId, receiptNumber)));
    }

    public void receiveLine(UUID lineId, BigDecimal quantity) {
        requireNotTerminal();
        lineById(lineId).receive(quantity);
        recalculateStatus();
    }

    /** Entry point for putaway-service. */
    public void allocateLineToPutAway(UUID lineId, BigDecimal quantity) {
        requireNotTerminal();
        lineById(lineId).allocateToPutAway(quantity);
        recalculateStatus();
    }

    public void cancelLine(UUID lineId) {
        requireNotTerminal();
        lineById(lineId).cancel();
        recalculateStatus();
    }

    public void cancel() {
        if (status.isTerminal()) {
            throw new BusinessRuleException("RECEIPT_" + status,
                    "Receipt %s is already %s".formatted(receiptNumber, status));
        }
        boolean anyPutAway = lines.stream()
                .anyMatch(l -> l.getPutAwayQuantity().signum() > 0);
        if (anyPutAway) {
            throw new BusinessRuleException("RECEIPT_PARTIALLY_PUT_AWAY",
                    "Receipt %s cannot be cancelled: stock from it is already put away"
                            .formatted(receiptNumber));
        }
        this.status = GoodsReceiptStatus.CANCELLED;
    }

    public boolean hasDiscrepancy() {
        return lines.stream().anyMatch(GoodsReceiptLine::hasDiscrepancy);
    }

    private void requireNotTerminal() {
        if (status.isTerminal()) {
            throw new BusinessRuleException("RECEIPT_" + status,
                    "Receipt %s is %s and cannot be changed".formatted(receiptNumber, status));
        }
    }

    /**
     * DRAFT until something is counted in, RECEIVED while work remains,
     * COMPLETED once every live line is fully put away. Derived, never set.
     */
    private void recalculateStatus() {
        if (status == GoodsReceiptStatus.CANCELLED) {
            return;
        }
        List<GoodsReceiptLine> liveLines = lines.stream()
                .filter(l -> !l.isCancelled())
                .toList();

        if (liveLines.isEmpty()) {
            this.status = GoodsReceiptStatus.DRAFT;
            return;
        }
        boolean anythingReceived = liveLines.stream()
                .anyMatch(l -> l.getReceivedQuantity().signum() > 0);
        if (!anythingReceived) {
            this.status = GoodsReceiptStatus.DRAFT;
            this.receivedAt = null;
            return;
        }
        if (receivedAt == null) {
            this.receivedAt = OffsetDateTime.now();
        }
        this.status = liveLines.stream().allMatch(GoodsReceiptLine::isFullyPutAway)
                ? GoodsReceiptStatus.COMPLETED
                : GoodsReceiptStatus.RECEIVED;
    }

    // --- accessors -------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public ReferenceType getReferenceType() {
        return referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public GoodsReceiptStatus getStatus() {
        return status;
    }

    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }

    public Long getVersion() {
        return version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<GoodsReceiptLine> getLines() {
        return List.copyOf(lines);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GoodsReceipt other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
