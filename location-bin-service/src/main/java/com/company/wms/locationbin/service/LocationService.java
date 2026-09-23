package com.company.wms.locationbin.service;

import com.company.wms.locationbin.domain.BinStatus;
import com.company.wms.locationbin.domain.Location;
import com.company.wms.locationbin.domain.LocationStatus;
import com.company.wms.locationbin.dto.CreateLocationRequest;
import com.company.wms.locationbin.dto.LocationResponse;
import com.company.wms.locationbin.dto.PageResponse;
import com.company.wms.locationbin.dto.UpdateLocationRequest;
import com.company.wms.locationbin.exception.BusinessRuleException;
import com.company.wms.locationbin.exception.DuplicateResourceException;
import com.company.wms.locationbin.exception.ResourceNotFoundException;
import com.company.wms.locationbin.mapper.LocationMapper;
import com.company.wms.locationbin.repository.BinRepository;
import com.company.wms.locationbin.repository.LocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private final LocationRepository locationRepository;
    private final BinRepository binRepository;

    public LocationService(LocationRepository locationRepository, BinRepository binRepository) {
        this.locationRepository = locationRepository;
        this.binRepository = binRepository;
    }

    @Transactional
    public LocationResponse create(CreateLocationRequest request) {
        String code = request.normalisedCode();

        // NOTE: warehouseId is NOT validated against warehouse-service.
        // When warehouse-service exists, inject a WarehouseClient here and check
        // that the warehouse exists and is ACTIVE. Do not query its database.
        if (locationRepository.existsByWarehouseIdAndLocationCode(request.warehouseId(), code)) {
            throw new DuplicateResourceException(
                    "LOCATION_CODE_ALREADY_EXISTS",
                    "Location code '%s' already exists in warehouse %s".formatted(code, request.warehouseId()));
        }

        Location location = new Location(
                request.warehouseId(),
                code,
                request.name().trim(),
                request.type(),
                request.statusOrDefault());

        Location saved = locationRepository.save(location);
        log.info("Created location id={} code={} warehouseId={}",
                saved.getId(), saved.getLocationCode(), saved.getWarehouseId());
        return LocationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public LocationResponse getById(UUID id) {
        return LocationMapper.toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<LocationResponse> listByWarehouse(UUID warehouseId, LocationStatus status, Pageable pageable) {
        Page<Location> page = (status == null)
                ? locationRepository.findByWarehouseId(warehouseId, pageable)
                : locationRepository.findByWarehouseIdAndStatus(warehouseId, status, pageable);
        return PageResponse.from(page, LocationMapper::toResponse);
    }

    @Transactional
    public LocationResponse update(UUID id, UpdateLocationRequest request) {
        Location location = findOrThrow(id);
        location.rename(request.name().trim());
        location.changeType(request.type());
        return LocationMapper.toResponse(location);
    }

    @Transactional
    public LocationResponse changeStatus(UUID id, LocationStatus newStatus) {
        Location location = findOrThrow(id);

        if (location.getStatus() == newStatus) {
            return LocationMapper.toResponse(location);
        }

        // A location must not be retired while it still has live bins, otherwise
        // put-away targets would silently point at a dead parent.
        if (newStatus == LocationStatus.INACTIVE) {
            long liveBins = binRepository.countByLocationIdAndStatusNot(id, BinStatus.INACTIVE);
            if (liveBins > 0) {
                throw new BusinessRuleException(
                        "LOCATION_HAS_ACTIVE_BINS",
                        "Location cannot be deactivated while %d non-inactive bin(s) remain".formatted(liveBins));
            }
        }

        location.changeStatus(newStatus);
        log.info("Location id={} status changed to {}", id, newStatus);
        return LocationMapper.toResponse(location);
    }

    /**
     * Not annotated: it is only ever called from the transactional methods above,
     * and a @Transactional annotation here would be silently ignored anyway
     * (self-invocation does not pass through the Spring proxy).
     */
    private Location findOrThrow(UUID id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "LOCATION_NOT_FOUND", "Location not found: " + id));
    }
}
