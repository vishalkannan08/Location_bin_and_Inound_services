package com.company.wms.inbound.domain;

public enum GoodsReceiptStatus {
    /** Created, expected quantities known, nothing physically counted yet. */
    DRAFT,
    /** At least one line has a received quantity. Lines are eligible for put-away. */
    RECEIVED,
    /** Every live line has been fully put away. Terminal. */
    COMPLETED,
    /** Abandoned. Terminal. */
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
