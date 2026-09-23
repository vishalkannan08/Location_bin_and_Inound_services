package com.company.wms.inbound.domain;

/**
 * What the goods arrived against. The requirement doc's sample payload uses
 * PURCHASE_ORDER; the rest are the obvious siblings. Confirm the real list with
 * the business before go-live.
 */
public enum ReferenceType {
    PURCHASE_ORDER,
    TRANSFER_ORDER,
    RETURN,
    MANUAL
}
