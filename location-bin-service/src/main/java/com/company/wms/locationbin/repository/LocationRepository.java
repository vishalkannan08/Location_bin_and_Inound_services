package com.company.wms.locationbin.repository;

import com.company.wms.locationbin.domain.Location;
import com.company.wms.locationbin.domain.LocationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {

    boolean existsByWarehouseIdAndLocationCode(UUID warehouseId, String locationCode);

    Optional<Location> findByWarehouseIdAndLocationCode(UUID warehouseId, String locationCode);

    Page<Location> findByWarehouseId(UUID warehouseId, Pageable pageable);

    Page<Location> findByWarehouseIdAndStatus(UUID warehouseId, LocationStatus status, Pageable pageable);
}
