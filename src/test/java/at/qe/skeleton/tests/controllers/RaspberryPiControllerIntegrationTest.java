package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.RaspberryPiCreateDTO;
import at.qe.skeleton.dtos.RaspberryPiDTO;
import at.qe.skeleton.dtos.RaspberryPiUpdateDTO;
import at.qe.skeleton.dtos.SensorStationDTO;
import at.qe.skeleton.helper.PiConfigYamlBuilder;
import at.qe.skeleton.mappers.RaspberryPiCreateMapper;
import at.qe.skeleton.mappers.RaspberryPiMapper;
import at.qe.skeleton.mappers.SensorStationMapper;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.models.SensorStation;
import at.qe.skeleton.services.PiRequestResult;
import at.qe.skeleton.services.RaspberryPiServerService;
import at.qe.skeleton.services.RaspberryPiService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@WithMockUser(roles = "EMPLOYEE")
class RaspberryPiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RaspberryPiService raspberryPiService;

    @MockitoBean
    private RaspberryPiMapper raspberryPiMapper;

    @MockitoBean
    private RaspberryPiServerService raspberryPiServerService;

    @MockitoBean
    private PiConfigYamlBuilder piConfigYamlBuilder;

    @MockitoBean
    private RaspberryPiCreateMapper raspberryPiCreateMapper;

    @MockitoBean
    private SensorStationMapper sensorStationMapper;

    private ObjectMapper objectMapper;

    private RaspberryPi pi1;
    private RaspberryPi pi2;

    private RaspberryPiDTO dto1;
    private RaspberryPiDTO dto2;

    private SensorStation station;
    private SensorStationDTO stationDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        pi1 = new RaspberryPi();
        ReflectionTestUtils.setField(pi1, "id", 1L);

        pi2 = new RaspberryPi();
        ReflectionTestUtils.setField(pi2, "id", 2L);

        dto1 = new RaspberryPiDTO(1L, "pi-1", "192.168.0.1", null, 10L, List.of());
        dto2 = new RaspberryPiDTO(2L, "pi-2", "192.168.0.2", null, 20L, List.of());

        station = new SensorStation();
        ReflectionTestUtils.setField(station, "id", 100L);

        stationDTO = new SensorStationDTO(
                100L, "station", "AA:BB", null, 10, 1L, 10L
        );
    }

    @Test
    void getAll_returns200() throws Exception {
        when(raspberryPiService.getAll()).thenReturn(List.of(pi1, pi2));
        when(raspberryPiMapper.mapTo(pi1)).thenReturn(dto1);
        when(raspberryPiMapper.mapTo(pi2)).thenReturn(dto2);

        mockMvc.perform(get("/api/bpi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));
    }

    @Test
    void getById_exists_returns200() throws Exception {
        when(raspberryPiService.getById(1L)).thenReturn(pi1);
        when(raspberryPiMapper.mapTo(pi1)).thenReturn(dto1);

        mockMvc.perform(get("/api/bpi/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(raspberryPiService.getById(99L))
                .thenThrow(new NotFoundException("Pi not found"));

        mockMvc.perform(get("/api/bpi/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_validPayload_returns201() throws Exception {
        RaspberryPiCreateDTO createDTO = new RaspberryPiCreateDTO(10L, "pi-1");

        when(raspberryPiCreateMapper.mapFrom(any())).thenReturn(pi1);
        when(raspberryPiService.create(pi1)).thenReturn(pi1);
        when(raspberryPiMapper.mapTo(pi1)).thenReturn(dto1);

        mockMvc.perform(post("/api/bpi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/bpi/1")))
                .andExpect(jsonPath("$.id", is(1)));

        verify(raspberryPiService).create(any());
    }

    @Test
    void create_invalidPayload_returns400() throws Exception {
        mockMvc.perform(post("/api/bpi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(raspberryPiService);
    }

    @Test
    void update_validPayload_returns200() throws Exception {
        RaspberryPiUpdateDTO updateDTO =
                new RaspberryPiUpdateDTO(null, "new-name", null, null, null);

        when(raspberryPiService.update(eq(1L), any()))
                .thenReturn(pi1);

        when(piConfigYamlBuilder.buildYaml(1L))
                .thenReturn("dummy-yaml");

        when(raspberryPiServerService.sendConfig(1L, "dummy-yaml"))
                .thenReturn(PiRequestResult.SUCCESS);

        when(raspberryPiMapper.mapTo(pi1))
                .thenReturn(dto1);

        mockMvc.perform(patch("/api/bpi/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(raspberryPiService).update(eq(1L), any());
        verify(piConfigYamlBuilder).buildYaml(1L);
        verify(raspberryPiServerService).sendConfig(1L, "dummy-yaml");
        verify(raspberryPiMapper).mapTo(pi1);
    }

    @Test
    void update_notFound_returns404() throws Exception {
        RaspberryPiUpdateDTO updateDTO =
                new RaspberryPiUpdateDTO("x", null, null, null, null);

        when(raspberryPiService.update(eq(99L), any()))
                .thenThrow(new NotFoundException("Pi not found"));

        mockMvc.perform(patch("/api/bpi/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_exists_returns204() throws Exception {
        doNothing().when(raspberryPiService).delete(1L);

        mockMvc.perform(delete("/api/bpi/1"))
                .andExpect(status().isNoContent());

        verify(raspberryPiService).delete(1L);
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(new NotFoundException("Pi not found"))
                .when(raspberryPiService).delete(99L);

        mockMvc.perform(delete("/api/bpi/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSensorStations_returns200() throws Exception {
        when(raspberryPiService.getSensorStations(1L))
                .thenReturn(List.of(station));

        when(sensorStationMapper.mapTo(station)).thenReturn(stationDTO);

        mockMvc.perform(get("/api/bpi/1/sensorstations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(100)));
    }

    @Test
    void getSensorStations_notFound_returns404() throws Exception {
        when(raspberryPiService.getSensorStations(99L))
                .thenThrow(new NotFoundException("Pi not found"));

        mockMvc.perform(get("/api/bpi/99/sensorstations"))
                .andExpect(status().isNotFound());
    }
}