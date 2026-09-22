package com.company.wms.locationbin.domain;

/**
 * Physical classification of a storage location inside a warehouse.
 * The BRD does not fix this list - extend it during the domain review.
 */
public enum LocationType {
    ZONE,
    AISLE,
    RACK,
    SHELF,
    FLOOR,
    STAGING,
    DOCK,
    QUARANTINE
}
