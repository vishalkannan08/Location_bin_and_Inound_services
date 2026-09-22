package com.company.wms.locationbin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "location")
public class Location {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Logical reference to warehouse-service. Intentionally NOT a JPA association
     * and NOT a foreign key - this service does not own the warehouse table.
     */
    @Column(name = "warehouse_id", nullable = false, updatable = false)
    private UUID warehouseId;

    @Column(name = "location_code", nullable = false, length = 50, updatable = false)
    private String locationCode;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private LocationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LocationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Location() {
        // required by JPA
    }

    public Location(UUID warehouseId, String locationCode, String name,
                    LocationType type, LocationStatus status) {
        this.id = UUID.randomUUID();
        this.warehouseId = warehouseId;
        this.locationCode = locationCode;
        this.name = name;
        this.type = type;
        this.status = status;
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

    public void rename(String name) {
        this.name = name;
    }

    public void changeType(LocationType type) {
        this.type = type;
    }

    public void changeStatus(LocationStatus status) {
        this.status = status;
    }

    public boolean acceptsNewBins() {
        return this.status.isUsable();
    }

    // --- accessors -------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public String getName() {
        return name;
    }

    public LocationType getType() {
        return type;
    }

    public LocationStatus getStatus() {
        return status;
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
        if (!(o instanceof Location other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
