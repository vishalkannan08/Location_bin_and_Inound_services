package com.company.wms.locationbin.controller;

import com.company.wms.locationbin.domain.LocationStatus;
import com.company.wms.locationbin.dto.CreateLocationRequest;
import com.company.wms.locationbin.dto.LocationResponse;
import com.company.wms.locationbin.dto.PageResponse;
import com.company.wms.locationbin.dto.UpdateLocationRequest;
import com.company.wms.locationbin.dto.UpdateLocationStatusRequest;
import com.company.wms.locationbin.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Locations", description = "Warehouse location master data")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping("/locations")
    @Operation(summary = "Create a location under a warehouse")
    public ResponseEntity<LocationResponse> create(@Valid @RequestBody CreateLocationRequest request,
                                                   UriComponentsBuilder uriBuilder) {
        LocationResponse response = locationService.create(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/locations/{id}").build(response.id()))
                .body(response);
    }

    @GetMapping("/locations/{id}")
    @Operation(summary = "Get a location by id")
    public LocationResponse getById(@PathVariable UUID id) {
        return locationService.getById(id);
    }

    @GetMapping("/warehouses/{warehouseId}/locations")
    @Operation(summary = "List locations of a warehouse, optionally filtered by status")
    public PageResponse<LocationResponse> listByWarehouse(
            @PathVariable UUID warehouseId,
            @RequestParam(required = false) LocationStatus status,
            @PageableDefault(size = 20, sort = "locationCode", direction = Sort.Direction.ASC) Pageable pageable) {
        return locationService.listByWarehouse(warehouseId, status, pageable);
    }

    @PutMapping("/locations/{id}")
    @Operation(summary = "Update a location's name and type (code and warehouse are immutable)")
    public LocationResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateLocationRequest request) {
        return locationService.update(id, request);
    }

    @PatchMapping("/locations/{id}/status")
    @Operation(summary = "Change a location's status")
    public LocationResponse changeStatus(@PathVariable UUID id,
                                         @Valid @RequestBody UpdateLocationStatusRequest request) {
        return locationService.changeStatus(id, request.status());
    }
}
