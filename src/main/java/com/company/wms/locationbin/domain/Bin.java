package com.company.wms.locationbin.domain;

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

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "bin")
public class Bin {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** A bin belongs to exactly one location, and that link never changes. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false, updatable = false)
    private Location location;

    @Column(name = "bin_code", nullable = false, length = 50, updatable = false)
    private String binCode;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BinStatus status;

    /**
     * Optimistic lock. Put-away will eventually contend on the same bin row from
     * concurrent workers; without this you get lost updates on status/capacity.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Bin() {
        // required by JPA
    }

    public Bin(Location location, String binCode, Integer capacity, BinStatus status) {
        this.id = UUID.randomUUID();
        this.location = location;
        this.binCode = binCode;
        this.capacity = capacity;
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

    public void changeCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public void changeStatus(BinStatus status) {
        this.status = status;
    }

    /**
     * A bin is a legal put-away target only when both the bin and its parent
     * location are usable. putaway-service reads this flag off the API response
     * instead of re-deriving the rule.
     */
    public boolean acceptsPutAway() {
        return status.acceptsPutAway() && location.getStatus().isUsable();
    }

    // --- accessors -------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public String getBinCode() {
        return binCode;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public BinStatus getStatus() {
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
        if (!(o instanceof Bin other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
