package com.company.wms.inbound.domain;

import com.company.wms.inbound.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "goods_receipt_line")
public class GoodsReceiptLine {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goods_receipt_id", nullable = false, updatable = false)
    private GoodsReceipt goodsReceipt;

    @Column(name = "line_number", nullable = false, updatable = false)
    private Integer lineNumber;

    /**
     * A SKU *code*, not an id. There is no product service in this increment, so
     * there is nothing to hold a UUID reference to.
     */
    @Column(name = "sku_id", nullable = false, length = 60, updatable = false)
    private String skuId;

    @Column(name = "expected_quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal expectedQuantity;

    @Column(name = "received_quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal receivedQuantity;

    /** How much of receivedQuantity putaway-service has already taken. */
    @Column(name = "put_away_quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal putAwayQuantity;

    @Column(name = "uom", nullable = false, length = 10, updatable = false)
    private String uom;

    @Column(name = "batch_number", length = 60)
    private String batchNumber;

    @Column(name = "serial_number", length = 60)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private GoodsReceiptLineStatus status;

    /**
     * Concurrent put-away workers will hit the same line. Without this, two
     * workers each allocating 60 of a 100-unit line both succeed and you have
     * put away 120 units that do not exist.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected GoodsReceiptLine() {
        // required by JPA
    }

    public GoodsReceiptLine(GoodsReceipt goodsReceipt, Integer lineNumber, String skuId,
                            BigDecimal expectedQuantity, BigDecimal receivedQuantity,
                            String uom, String batchNumber, String serialNumber) {
        this.id = UUID.randomUUID();
        this.goodsReceipt = goodsReceipt;
        this.lineNumber = lineNumber;
        this.skuId = skuId;
        this.expectedQuantity = expectedQuantity;
        this.receivedQuantity = receivedQuantity;
        this.putAwayQuantity = BigDecimal.ZERO;
        this.uom = uom;
        this.batchNumber = batchNumber;
        this.serialNumber = serialNumber;
        recalculateStatus();
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

    /** Record the physical count for this line. */
    public void receive(BigDecimal quantity) {
        if (status == GoodsReceiptLineStatus.CANCELLED) {
            throw new BusinessRuleException("LINE_CANCELLED",
                    "Line %d is cancelled and cannot be received".formatted(lineNumber));
        }
        if (putAwayQuantity.compareTo(quantity) > 0) {
            throw new BusinessRuleException("RECEIVED_BELOW_PUT_AWAY",
                    ("Line %d already has %s put away; received quantity cannot be "
                            + "reduced to %s").formatted(lineNumber, putAwayQuantity, quantity));
        }
        this.receivedQuantity = quantity;
        recalculateStatus();
    }

    /**
     * Called by putaway-service when a put-away task completes.
     * Section 9: put-away quantity cannot exceed eligible received quantity.
     */
    public void allocateToPutAway(BigDecimal quantity) {
        if (status == GoodsReceiptLineStatus.CANCELLED) {
            throw new BusinessRuleException("LINE_CANCELLED",
                    "Line %d is cancelled and cannot be put away".formatted(lineNumber));
        }
        BigDecimal newTotal = putAwayQuantity.add(quantity);
        if (newTotal.compareTo(receivedQuantity) > 0) {
            throw new BusinessRuleException("PUT_AWAY_EXCEEDS_RECEIVED",
                    ("Cannot put away %s: line %d has received %s with %s already put away "
                            + "(%s remaining)").formatted(
                            quantity, lineNumber, receivedQuantity, putAwayQuantity, remainingQuantity()));
        }
        this.putAwayQuantity = newTotal;
        recalculateStatus();
    }

    public void cancel() {
        if (putAwayQuantity.signum() > 0) {
            throw new BusinessRuleException("LINE_ALREADY_PUT_AWAY",
                    ("Line %d cannot be cancelled: %s has already been put away")
                            .formatted(lineNumber, putAwayQuantity));
        }
        this.status = GoodsReceiptLineStatus.CANCELLED;
    }

    /** What putaway-service is still allowed to take from this line. */
    public BigDecimal remainingQuantity() {
        return receivedQuantity.subtract(putAwayQuantity);
    }

    /** Received more or less than the supplier said they sent. */
    public boolean hasDiscrepancy() {
        return status != GoodsReceiptLineStatus.PENDING
                && expectedQuantity.compareTo(receivedQuantity) != 0;
    }

    public boolean isFullyPutAway() {
        return status == GoodsReceiptLineStatus.PUT_AWAY;
    }

    public boolean isCancelled() {
        return status == GoodsReceiptLineStatus.CANCELLED;
    }

    /**
     * Status is derived from the quantities, never set directly, so the two can
     * never disagree.
     */
    private void recalculateStatus() {
        if (status == GoodsReceiptLineStatus.CANCELLED) {
            return;
        }
        if (receivedQuantity.signum() == 0) {
            this.status = GoodsReceiptLineStatus.PENDING;
        } else if (putAwayQuantity.signum() == 0) {
            this.status = GoodsReceiptLineStatus.RECEIVED;
        } else if (putAwayQuantity.compareTo(receivedQuantity) < 0) {
            this.status = GoodsReceiptLineStatus.PARTIALLY_PUT_AWAY;
        } else {
            this.status = GoodsReceiptLineStatus.PUT_AWAY;
        }
    }

    // --- accessors -------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public GoodsReceipt getGoodsReceipt() {
        return goodsReceipt;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public String getSkuId() {
        return skuId;
    }

    public BigDecimal getExpectedQuantity() {
        return expectedQuantity;
    }

    public BigDecimal getReceivedQuantity() {
        return receivedQuantity;
    }

    public BigDecimal getPutAwayQuantity() {
        return putAwayQuantity;
    }

    public String getUom() {
        return uom;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public GoodsReceiptLineStatus getStatus() {
        return status;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GoodsReceiptLine other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
