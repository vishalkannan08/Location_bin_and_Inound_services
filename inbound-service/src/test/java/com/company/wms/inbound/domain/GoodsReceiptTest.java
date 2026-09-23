package com.company.wms.inbound.domain;

import com.company.wms.inbound.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure domain tests - no Spring, no database, no mocks. The quantity rules in
 * section 9 live in the entities, so this is where they are tested.
 */
class GoodsReceiptTest {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal SIXTY = new BigDecimal("60");
    private static final BigDecimal FORTY = new BigDecimal("40");

    @Test
    void newReceiptWithNoCountedStockIsDraft() {
        GoodsReceipt receipt = receipt();
        receipt.addLine("SKU-1001", HUNDRED, BigDecimal.ZERO, "EA", null, null);

        assertThat(receipt.getStatus()).isEqualTo(GoodsReceiptStatus.DRAFT);
        assertThat(receipt.getReceivedAt()).isNull();
        assertThat(receipt.getLines().get(0).getStatus()).isEqualTo(GoodsReceiptLineStatus.PENDING);
    }

    @Test
    void receiptWithCountedStockIsReceivedAndStampsReceivedAt() {
        GoodsReceipt receipt = receipt();
        receipt.addLine("SKU-1001", HUNDRED, HUNDRED, "EA", null, null);

        assertThat(receipt.getStatus()).isEqualTo(GoodsReceiptStatus.RECEIVED);
        assertThat(receipt.getReceivedAt()).isNotNull();
        assertThat(receipt.getLines().get(0).getStatus()).isEqualTo(GoodsReceiptLineStatus.RECEIVED);
    }

    @Test
    void lineNumbersAreAssignedSequentially() {
        GoodsReceipt receipt = receipt();
        receipt.addLine("SKU-1001", HUNDRED, HUNDRED, "EA", null, null);
        receipt.addLine("SKU-1002", FORTY, FORTY, "EA", null, null);

        assertThat(receipt.getLines())
                .extracting(GoodsReceiptLine::getLineNumber)
                .containsExactly(1, 2);
    }

    @Test
    void partialPutAwayMovesLineToPartiallyPutAway() {
        GoodsReceipt receipt = receipt();
        GoodsReceiptLine line = receipt.addLine("SKU-1001", HUNDRED, HUNDRED, "EA", null, null);

        receipt.allocateLineToPutAway(line.getId(), SIXTY);

        assertThat(line.getStatus()).isEqualTo(GoodsReceiptLineStatus.PARTIALLY_PUT_AWAY);
        assertThat(line.remainingQuantity()).isEqualByComparingTo(FORTY);
        assertThat(receipt.getStatus()).isEqualTo(GoodsReceiptStatus.RECEIVED);
    }

    @Test
    void receiptCompletesOnlyWhenEveryLineIsFullyPutAway() {
        GoodsReceipt receipt = receipt();
        GoodsReceiptLine first = receipt.addLine("SKU-1001", HUNDRED, HUNDRED, "EA", null, null);
        GoodsReceiptLine second = receipt.addLine("SKU-1002", FORTY, FORTY, "EA", null, null);

        receipt.allocateLineToPutAway(first.getId(), HUNDRED);
        assertThat(receipt.getStatus()).isEqualTo(GoodsReceiptStatus.RECEIVED);

        receipt.allocateLineToPutAway(second.getId(), FORTY);
        assertThat(receipt.getStatus()).isEqualTo(GoodsReceiptStatus.COMPLETED);
    }

    @Test
    void putAwayCannotExceedReceivedQuantity() {
        GoodsReceipt receipt = receipt();
        GoodsReceiptLine line = receipt.addLine("SKU-1001", HUNDRED, HUNDRED, "EA", null, null);
        receipt.allocateLineToPutAway(line.getId(), SIXTY);

        assertThatThrownBy(() -> receipt.allocateLineToPutAway(line.getId(), new BigDecimal("50")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("40");

        assertThat(line.getPutAwayQuantity()).isEqualByComparingTo(SIXTY);
    }

    @Test
    void putAwayCannotHappenBeforeStockIsCountedIn() {
        GoodsReceipt receipt = receipt();
        GoodsReceiptLine line = receipt.addLine("SKU-1001", HUNDRED, BigDecimal.ZERO, "EA", null, null);

        assertThatThrownBy(() -> receipt.allocateLineToPutAway(line.getId(), BigDecimal.ONE))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shortShipmentIsFlaggedAsADiscrepancy() {
        GoodsReceipt receipt = receipt();
        receipt.addLine("SKU-1001", HUNDRED, new BigDecimal("95"), "EA", null, null);

        assertThat(receipt.hasDiscrepancy()).isTrue();
        assertThat(receipt.getLines().get(0).hasDiscrepancy()).isTrue();
    }

    @Test
    void receivedQuantityCannotBeReducedBelowWhatIsAlreadyPutAway() {
        GoodsReceipt receipt = receipt();
        GoodsReceiptLine line = receipt.addLine("SKU-1001", HUNDRED, HUNDRED, "EA", null, null);
        receipt.allocateLineToPutAway(line.getId(), SIXTY);

        assertThatThrownBy(() -> receipt.receiveLine(line.getId(), new BigDecimal("50")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("put away");
    }

    @Test
    void receiptCannotBeCancelledOnceStockIsPutAway() {
        GoodsReceipt receipt = receipt();
        GoodsReceiptLine line = receipt.addLine("SKU-1001", HUNDRED, HUNDRED, "EA", null, null);
        receipt.allocateLineToPutAway(line.getId(), BigDecimal.ONE);

        assertThatThrownBy(receipt::cancel)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already put away");
    }

    @Test
    void cancelledLinesAreExcludedFromTheCompletionCheck() {
        GoodsReceipt receipt = receipt();
        GoodsReceiptLine live = receipt.addLine("SKU-1001", HUNDRED, HUNDRED, "EA", null, null);
        GoodsReceiptLine dead = receipt.addLine("SKU-9999", FORTY, BigDecimal.ZERO, "EA", null, null);

        receipt.cancelLine(dead.getId());
        receipt.allocateLineToPutAway(live.getId(), HUNDRED);

        assertThat(receipt.getStatus()).isEqualTo(GoodsReceiptStatus.COMPLETED);
    }

    @Test
    void decimalQuantitiesWork() {
        GoodsReceipt receipt = receipt();
        GoodsReceiptLine line = receipt.addLine(
                "SKU-BULK", new BigDecimal("12.500"), new BigDecimal("12.500"), "KG", null, null);

        receipt.allocateLineToPutAway(line.getId(), new BigDecimal("7.250"));

        assertThat(line.remainingQuantity()).isEqualByComparingTo(new BigDecimal("5.250"));
    }

    private static GoodsReceipt receipt() {
        return new GoodsReceipt("GR-000001", UUID.randomUUID(),
                ReferenceType.PURCHASE_ORDER, "PO-10001");
    }
}
