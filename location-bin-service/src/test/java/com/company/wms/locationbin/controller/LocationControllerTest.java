package com.company.wms.locationbin.controller;

import com.company.wms.locationbin.domain.LocationStatus;
import com.company.wms.locationbin.domain.LocationType;
import com.company.wms.locationbin.dto.LocationResponse;
import com.company.wms.locationbin.exception.ResourceNotFoundException;
import com.company.wms.locationbin.service.LocationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationController.class)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LocationService locationService;

    @Test
    void createReturns201WithLocationHeader() throws Exception {
        UUID id = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        when(locationService.create(any())).thenReturn(new LocationResponse(
                id, warehouseId, "A1-RACK-01", "Rack 1", LocationType.RACK, LocationStatus.ACTIVE,
                OffsetDateTime.now(), OffsetDateTime.now()));

        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", warehouseId,
                                "locationCode", "A1-RACK-01",
                                "name", "Rack 1",
                                "type", "RACK"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/locations/" + id))
                .andExpect(jsonPath("$.locationCode").value("A1-RACK-01"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createReturns400WithFieldErrorsWhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "locationCode", "",
                                "name", "Rack 1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void createReturns400WhenTypeIsNotAKnownEnumValue() throws Exception {
        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "warehouseId", UUID.randomUUID(),
                                "locationCode", "A1",
                                "name", "Rack 1",
                                "type", "NOT_A_TYPE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void getReturns404InStandardErrorShape() throws Exception {
        UUID id = UUID.randomUUID();
        when(locationService.getById(id))
                .thenThrow(new ResourceNotFoundException("LOCATION_NOT_FOUND", "Location not found: " + id));

        mockMvc.perform(get("/api/v1/locations/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOCATION_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/v1/locations/" + id));
    }

    @Test
    void getReturns400WhenIdIsNotAUuid() throws Exception {
        mockMvc.perform(get("/api/v1/locations/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }
}
