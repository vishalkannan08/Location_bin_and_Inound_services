package com.company.wms.locationbin.service;

import com.company.wms.locationbin.domain.BinStatus;
import com.company.wms.locationbin.domain.Location;
import com.company.wms.locationbin.domain.LocationStatus;
import com.company.wms.locationbin.domain.LocationType;
import com.company.wms.locationbin.dto.CreateLocationRequest;
import com.company.wms.locationbin.dto.LocationResponse;
import com.company.wms.locationbin.exception.BusinessRuleException;
import com.company.wms.locationbin.exception.DuplicateResourceException;
import com.company.wms.locationbin.exception.ResourceNotFoundException;
import com.company.wms.locationbin.repository.BinRepository;
import com.company.wms.locationbin.repository.LocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private BinRepository binRepository;

    @InjectMocks
    private LocationService locationService;

    private final UUID warehouseId = UUID.randomUUID();

    @Test
    void createsLocationWithUppercasedCodeAndDefaultActiveStatus() {
        CreateLocationRequest request = new CreateLocationRequest(
                warehouseId, " a1-rack-01 ", "  Aisle 1 Rack 1  ", LocationType.RACK, null);

        when(locationRepository.existsByWarehouseIdAndLocationCode(warehouseId, "A1-RACK-01")).thenReturn(false);
        when(locationRepository.save(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));

        LocationResponse response = locationService.create(request);

        ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        verify(locationRepository).save(captor.capture());

        assertThat(captor.getValue().getLocationCode()).isEqualTo("A1-RACK-01");
        assertThat(captor.getValue().getName()).isEqualTo("Aisle 1 Rack 1");
        assertThat(response.status()).isEqualTo(LocationStatus.ACTIVE);
        assertThat(response.warehouseId()).isEqualTo(warehouseId);
    }

    @Test
    void rejectsDuplicateLocationCodeWithinSameWarehouse() {
        CreateLocationRequest request = new CreateLocationRequest(
                warehouseId, "A1-RACK-01", "Rack 1", LocationType.RACK, null);

        when(locationRepository.existsByWarehouseIdAndLocationCode(warehouseId, "A1-RACK-01")).thenReturn(true);

        assertThatThrownBy(() -> locationService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(locationRepository, never()).save(any());
    }

    @Test
    void throwsNotFoundForUnknownId() {
        UUID id = UUID.randomUUID();
        when(locationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void blocksDeactivationWhileLiveBinsRemain() {
        Location location = activeLocation();
        when(locationRepository.findById(location.getId())).thenReturn(Optional.of(location));
        when(binRepository.countByLocationIdAndStatusNot(location.getId(), BinStatus.INACTIVE)).thenReturn(3L);

        assertThatThrownBy(() -> locationService.changeStatus(location.getId(), LocationStatus.INACTIVE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("3");

        assertThat(location.getStatus()).isEqualTo(LocationStatus.ACTIVE);
    }

    @Test
    void allowsDeactivationWhenNoLiveBinsRemain() {
        Location location = activeLocation();
        when(locationRepository.findById(location.getId())).thenReturn(Optional.of(location));
        when(binRepository.countByLocationIdAndStatusNot(location.getId(), BinStatus.INACTIVE)).thenReturn(0L);

        LocationResponse response = locationService.changeStatus(location.getId(), LocationStatus.INACTIVE);

        assertThat(response.status()).isEqualTo(LocationStatus.INACTIVE);
    }

    @Test
    void blockingALocationDoesNotRequireEmptyBins() {
        Location location = activeLocation();
        when(locationRepository.findById(location.getId())).thenReturn(Optional.of(location));

        LocationResponse response = locationService.changeStatus(location.getId(), LocationStatus.BLOCKED);

        assertThat(response.status()).isEqualTo(LocationStatus.BLOCKED);
        verify(binRepository, never()).countByLocationIdAndStatusNot(any(), any());
    }

    private Location activeLocation() {
        return new Location(warehouseId, "A1-RACK-01", "Rack 1", LocationType.RACK, LocationStatus.ACTIVE);
    }
}
