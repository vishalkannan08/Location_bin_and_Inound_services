package com.company.wms.locationbin.repository;

import com.company.wms.locationbin.domain.Bin;
import com.company.wms.locationbin.domain.BinStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BinRepository extends JpaRepository<Bin, UUID> {

    boolean existsByLocationIdAndBinCode(UUID locationId, String binCode);

    Page<Bin> findByLocationId(UUID locationId, Pageable pageable);

    Page<Bin> findByLocationIdAndStatus(UUID locationId, BinStatus status, Pageable pageable);

    long countByLocationIdAndStatusNot(UUID locationId, BinStatus status);

    /**
     * Fetches the parent location in the same query. Bin.acceptsPutAway() touches
     * location.status, so a plain findById would trigger a lazy load (or fail
     * outside the transaction, since open-in-view is disabled).
     */
    @Query("select b from Bin b join fetch b.location where b.id = :id")
    Optional<Bin> findByIdWithLocation(@Param("id") UUID id);
}
