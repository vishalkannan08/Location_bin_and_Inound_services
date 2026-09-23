package com.company.wms.inbound.domain;

public enum GoodsReceiptLineStatus {
    /** Expected but nothing counted in yet. */
    PENDING,
    /** Counted in, nothing put away. Fully eligible for put-away. */
    RECEIVED,
    /** Some of the received quantity is put away, some still on the dock. */
    PARTIALLY_PUT_AWAY,
    /** Everything received has been put away. */
    PUT_AWAY,
    /** Line abandoned. Not eligible for put-away. */
    CANCELLED
}
