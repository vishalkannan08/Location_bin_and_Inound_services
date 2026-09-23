package com.company.wms.locationbin.controller;

import com.company.wms.locationbin.domain.BinStatus;
import com.company.wms.locationbin.dto.BinResponse;
import com.company.wms.locationbin.dto.CreateBinRequest;
import com.company.wms.locationbin.dto.PageResponse;
import com.company.wms.locationbin.dto.UpdateBinRequest;
import com.company.wms.locationbin.dto.UpdateBinStatusRequest;
import com.company.wms.locationbin.service.BinService;
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
@Tag(name = "Bins", description = "Bin master data within a location")
public class BinController {

    private final BinService binService;

    public BinController(BinService binService) {
        this.binService = binService;
    }

    @PostMapping("/bins")
    @Operation(summary = "Create a bin under a location")
    public ResponseEntity<BinResponse> create(@Valid @RequestBody CreateBinRequest request,
                                              UriComponentsBuilder uriBuilder) {
        BinResponse response = binService.create(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/bins/{id}").build(response.id()))
                .body(response);
    }

    /**
     * putaway-service calls this to validate a target bin. The acceptsPutAway
     * flag on the response is the authoritative answer.
     */
    @GetMapping("/bins/{id}")
    @Operation(summary = "Get a bin by id, including its put-away eligibility")
    public BinResponse getById(@PathVariable UUID id) {
        return binService.getById(id);
    }

    @GetMapping("/locations/{locationId}/bins")
    @Operation(summary = "List bins of a location, optionally filtered by status")
    public PageResponse<BinResponse> listByLocation(
            @PathVariable UUID locationId,
            @RequestParam(required = false) BinStatus status,
            @PageableDefault(size = 20, sort = "binCode", direction = Sort.Direction.ASC) Pageable pageable) {
        return binService.listByLocation(locationId, status, pageable);
    }

    @PutMapping("/bins/{id}")
    @Operation(summary = "Update a bin's capacity (code and location are immutable)")
    public BinResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateBinRequest request) {
        return binService.update(id, request);
    }

    @PatchMapping("/bins/{id}/status")
    @Operation(summary = "Change a bin's status")
    public BinResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody UpdateBinStatusRequest request) {
        return binService.changeStatus(id, request.status());
    }
}
