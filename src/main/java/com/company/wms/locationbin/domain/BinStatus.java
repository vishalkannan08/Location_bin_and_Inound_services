package com.company.wms.locationbin.domain;

public enum BinStatus {
    /** Accepts put-away. */
    AVAILABLE,
    /** Holds stock but still has capacity. Accepts put-away. */
    OCCUPIED,
    /** At capacity. Rejects put-away. */
    FULL,
    /** Temporarily unusable. Rejects put-away. */
    BLOCKED,
    /** Retired. Rejects put-away. */
    INACTIVE;

    /**
     * Used by putaway-service (via GET /api/v1/bins/{id}) to decide whether a bin
     * is a legal put-away target. Keep this rule here, not in the caller.
     */
    public boolean acceptsPutAway() {
        return this == AVAILABLE || this == OCCUPIED;
    }
}
