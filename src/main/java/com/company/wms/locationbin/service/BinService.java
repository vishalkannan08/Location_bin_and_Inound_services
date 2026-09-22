package com.company.wms.locationbin.service;

import com.company.wms.locationbin.domain.Bin;
import com.company.wms.locationbin.domain.BinStatus;
import com.company.wms.locationbin.domain.Location;
import com.company.wms.locationbin.dto.BinResponse;
import com.company.wms.locationbin.dto.CreateBinRequest;
import com.company.wms.locationbin.dto.PageResponse;
import com.company.wms.locationbin.dto.UpdateBinRequest;
import com.company.wms.locationbin.exception.BusinessRuleException;
import com.company.wms.locationbin.exception.DuplicateResourceException;
import com.company.wms.locationbin.exception.ResourceNotFoundException;
import com.company.wms.locationbin.mapper.BinMapper;
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
public class BinService {

    private static final Logger log = LoggerFactory.getLogger(BinService.class);

    private final BinRepository binRepository;
    private final LocationRepository locationRepository;

    public BinService(BinRepository binRepository, LocationRepository locationRepository) {
        this.binRepository = binRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional
    public BinResponse create(CreateBinRequest request) {
        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "LOCATION_NOT_FOUND", "Location not found: " + request.locationId()));

        if (!location.acceptsNewBins()) {
            throw new BusinessRuleException(
                    "LOCATION_NOT_ACTIVE",
                    "Bins can only be created under an ACTIVE location. Location %s is %s"
                            .formatted(location.getId(), location.getStatus()));
        }

        String code = request.normalisedCode();
        if (binRepository.existsByLocationIdAndBinCode(location.getId(), code)) {
            throw new DuplicateResourceException(
                    "BIN_CODE_ALREADY_EXISTS",
                    "Bin code '%s' already exists in location %s".formatted(code, location.getId()));
        }

        Bin bin = new Bin(location, code, request.capacity(), request.statusOrDefault());
        Bin saved = binRepository.save(bin);
        log.info("Created bin id={} code={} locationId={}", saved.getId(), saved.getBinCode(), location.getId());
        return BinMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BinResponse getById(UUID id) {
        return BinMapper.toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<BinResponse> listByLocation(UUID locationId, BinStatus status, Pageable pageable) {
        if (!locationRepository.existsById(locationId)) {
            throw new ResourceNotFoundException("LOCATION_NOT_FOUND", "Location not found: " + locationId);
        }
        Page<Bin> page = (status == null)
                ? binRepository.findByLocationId(locationId, pageable)
                : binRepository.findByLocationIdAndStatus(locationId, status, pageable);
        return PageResponse.from(page, BinMapper::toResponse);
    }

    @Transactional
    public BinResponse update(UUID id, UpdateBinRequest request) {
        Bin bin = findOrThrow(id);
        bin.changeCapacity(request.capacity());
        return BinMapper.toResponse(bin);
    }

    @Transactional
    public BinResponse changeStatus(UUID id, BinStatus newStatus) {
        Bin bin = findOrThrow(id);

        if (bin.getStatus() == newStatus) {
            return BinMapper.toResponse(bin);
        }

        // Reactivating a bin under a dead location would create an unreachable
        // put-away target.
        if (newStatus.acceptsPutAway() && !bin.getLocation().getStatus().isUsable()) {
            throw new BusinessRuleException(
                    "LOCATION_NOT_ACTIVE",
                    "Bin cannot be set to %s while its location is %s"
                            .formatted(newStatus, bin.getLocation().getStatus()));
        }

        bin.changeStatus(newStatus);
        log.info("Bin id={} status changed to {}", id, newStatus);
        return BinMapper.toResponse(bin);
    }

    private Bin findOrThrow(UUID id) {
        return binRepository.findByIdWithLocation(id)
                .orElseThrow(() -> new ResourceNotFoundException("BIN_NOT_FOUND", "Bin not found: " + id));
    }
}
