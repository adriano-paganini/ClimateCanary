package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.SensorStationCreateDTO;
import at.qe.skeleton.dtos.SensorStationDTO;
import at.qe.skeleton.dtos.SensorStationUpdateDTO;
import at.qe.skeleton.mappers.SensorStationCreateMapper;
import at.qe.skeleton.mappers.SensorStationMapper;
import at.qe.skeleton.models.DeviceStatus;
import at.qe.skeleton.models.SensorStation;
import at.qe.skeleton.services.SensorStationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@WithMockUser(roles = "EMPLOYEE")
public class SensorStationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SensorStationService sensorStationService;

    @MockitoBean
    private SensorStationMapper sensorStationMapper;

    @MockitoBean
    private SensorStationCreateMapper sensorStationCreateMapper;

    private ObjectMapper objectMapper;

    private SensorStation station1;
    private SensorStation station2;

    private SensorStationDTO dto1;
    private SensorStationDTO dto2;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        station1 = new SensorStation();
        ReflectionTestUtils.setField(station1, "id", 1L);

        station2 = new SensorStation();
        ReflectionTestUtils.setField(station2, "id", 2L);

        dto1 = new SensorStationDTO(
                1L,
                "Station A",
                "AA:BB:CC",
                DeviceStatus.ONLINE,
                60,
                1L,
                1L
        );

        dto2 = new SensorStationDTO(
                2L,
                "Station B",
                "DD:EE:FF",
                DeviceStatus.OFFLINE,
                120,
                2L,
                2L
        );
    }

    @Test
    void getAll_returns200WithStations() throws Exception {
        when(sensorStationService.getAll()).thenReturn(List.of(station1, station2));
        when(sensorStationMapper.mapTo(station1)).thenReturn(dto1);
        when(sensorStationMapper.mapTo(station2)).thenReturn(dto2);

        mockMvc.perform(get("/api/sensorstation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));

        verify(sensorStationService).getAll();
    }

    @Test
    void getById_exists_returns200() throws Exception {
        when(sensorStationService.getById(1L)).thenReturn(station1);
        when(sensorStationMapper.mapTo(station1)).thenReturn(dto1);

        mockMvc.perform(get("/api/sensorstation/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(sensorStationService).getById(1L);
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(sensorStationService.getById(99L))
                .thenThrow(new NotFoundException("SensorStation not found"));

        mockMvc.perform(get("/api/sensorstation/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_validPayload_returns201() throws Exception {
        SensorStationCreateDTO createDTO = new SensorStationCreateDTO(
                "Station A",
                DeviceStatus.AVAILABLE,
                "AA:BB:CC",
                60,
                1L,
                1L
        );

        when(sensorStationCreateMapper.mapFrom(any())).thenReturn(station1);
        when(sensorStationService.create(station1)).thenReturn(station1);
        when(sensorStationMapper.mapTo(station1)).thenReturn(dto1);

        mockMvc.perform(post("/api/sensorstation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/sensorstation/1")))
                .andExpect(jsonPath("$.id", is(1)));

        verify(sensorStationService).create(any());
    }

    @Test
    void update_validPayload_returns200() throws Exception {
        SensorStationUpdateDTO updateDTO = new SensorStationUpdateDTO(
                1L,
                1L,
                "Updated",
                DeviceStatus.AVAILABLE
        );

        when(sensorStationService.update(eq(1L), any())).thenReturn(station1);
        when(sensorStationMapper.mapTo(station1)).thenReturn(dto1);

        mockMvc.perform(patch("/api/sensorstation/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(sensorStationService).update(eq(1L), any());
    }

    @Test
    void update_notFound_returns404() throws Exception {
        when(sensorStationService.update(eq(99L), any()))
                .thenThrow(new NotFoundException("SensorStation not found"));

        mockMvc.perform(patch("/api/sensorstation/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SensorStationUpdateDTO(null, null, null, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_exists_returns204() throws Exception {
        doNothing().when(sensorStationService).delete(1L);

        mockMvc.perform(delete("/api/sensorstation/1"))
                .andExpect(status().isNoContent());

        verify(sensorStationService).delete(1L);
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(new NotFoundException("SensorStation not found"))
                .when(sensorStationService).delete(99L);

        mockMvc.perform(delete("/api/sensorstation/99"))
                .andExpect(status().isNotFound());
    }
}