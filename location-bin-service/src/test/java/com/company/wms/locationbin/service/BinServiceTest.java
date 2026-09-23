package com.company.wms.locationbin.service;

import com.company.wms.locationbin.domain.Bin;
import com.company.wms.locationbin.domain.BinStatus;
import com.company.wms.locationbin.domain.Location;
import com.company.wms.locationbin.domain.LocationStatus;
import com.company.wms.locationbin.domain.LocationType;
import com.company.wms.locationbin.dto.BinResponse;
import com.company.wms.locationbin.dto.CreateBinRequest;
import com.company.wms.locationbin.exception.BusinessRuleException;
import com.company.wms.locationbin.exception.DuplicateResourceException;
import com.company.wms.locationbin.exception.ResourceNotFoundException;
import com.company.wms.locationbin.repository.BinRepository;
import com.company.wms.locationbin.repository.LocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BinServiceTest {

    @Mock
    private BinRepository binRepository;

    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private BinService binService;

    @Test
    void createsBinUnderActiveLocation() {
        Location location = location(LocationStatus.ACTIVE);
        CreateBinRequest request = new CreateBinRequest(location.getId(), "bin-001", 120, null);

        when(locationRepository.findById(location.getId())).thenReturn(Optional.of(location));
        when(binRepository.existsByLocationIdAndBinCode(location.getId(), "BIN-001")).thenReturn(false);
        when(binRepository.save(any(Bin.class))).thenAnswer(inv -> inv.getArgument(0));

        BinResponse response = binService.create(request);

        assertThat(response.binCode()).isEqualTo("BIN-001");
        assertThat(response.capacity()).isEqualTo(120);
        assertThat(response.status()).isEqualTo(BinStatus.AVAILABLE);
        assertThat(response.acceptsPutAway()).isTrue();
        assertThat(response.warehouseId()).isEqualTo(location.getWarehouseId());
    }

    @Test
    void rejectsBinCreationUnderBlockedLocation() {
        Location location = location(LocationStatus.BLOCKED);
        CreateBinRequest request = new CreateBinRequest(location.getId(), "BIN-001", 100, null);

        when(locationRepository.findById(location.getId())).thenReturn(Optional.of(location));

        assertThatThrownBy(() -> binService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ACTIVE");

        verify(binRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateBinCodeWithinSameLocation() {
        Location location = location(LocationStatus.ACTIVE);
        CreateBinRequest request = new CreateBinRequest(location.getId(), "BIN-001", 100, null);

        when(locationRepository.findById(location.getId())).thenReturn(Optional.of(location));
        when(binRepository.existsByLocationIdAndBinCode(location.getId(), "BIN-001")).thenReturn(true);

        assertThatThrownBy(() -> binService.create(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void rejectsBinCreationForUnknownLocation() {
        UUID unknown = UUID.randomUUID();
        when(locationRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> binService.create(new CreateBinRequest(unknown, "BIN-001", 100, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void blockedBinIsNotAPutAwayTarget() {
        Location location = location(LocationStatus.ACTIVE);
        Bin bin = new Bin(location, "BIN-001", 100, BinStatus.BLOCKED);

        when(binRepository.findByIdWithLocation(bin.getId())).thenReturn(Optional.of(bin));

        assertThat(binService.getById(bin.getId()).acceptsPutAway()).isFalse();
    }

    @Test
    void binUnderBlockedLocationIsNotAPutAwayTargetEvenWhenAvailable() {
        Location location = location(LocationStatus.BLOCKED);
        Bin bin = new Bin(location, "BIN-001", 100, BinStatus.AVAILABLE);

        when(binRepository.findByIdWithLocation(bin.getId())).thenReturn(Optional.of(bin));

        assertThat(binService.getById(bin.getId()).acceptsPutAway()).isFalse();
    }

    @Test
    void cannotReactivateBinWhileLocationIsBlocked() {
        Location location = location(LocationStatus.BLOCKED);
        Bin bin = new Bin(location, "BIN-001", 100, BinStatus.BLOCKED);

        when(binRepository.findByIdWithLocation(bin.getId())).thenReturn(Optional.of(bin));

        assertThatThrownBy(() -> binService.changeStatus(bin.getId(), BinStatus.AVAILABLE))
                .isInstanceOf(BusinessRuleException.class);
    }

    private Location location(LocationStatus status) {
        return new Location(UUID.randomUUID(), "A1-RACK-01", "Rack 1", LocationType.RACK, status);
    }
}
