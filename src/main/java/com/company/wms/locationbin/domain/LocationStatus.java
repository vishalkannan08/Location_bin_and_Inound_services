package com.company.wms.locationbin.domain;

public enum LocationStatus {
    /** Usable: new bins may be created and put-away may target its bins. */
    ACTIVE,
    /** Retired. No new bins, no put-away. */
    INACTIVE,
    /** Temporarily unusable (damage, stock count, safety). No put-away. */
    BLOCKED;

    public boolean isUsable() {
        return this == ACTIVE;
    }
}
