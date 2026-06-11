package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.RPMeasurementDTO;
import at.qe.skeleton.dtos.RaspberryPiUpdateDTO;
import at.qe.skeleton.dtos.ViolationActiveDTO;
import at.qe.skeleton.dtos.ViolationResolvedDTO;
import at.qe.skeleton.helper.PiConfigYamlBuilder;
import at.qe.skeleton.models.DeviceStatus;
import at.qe.skeleton.models.Metric;
import at.qe.skeleton.scheduled.AvailableSensorStationCleaner;
import at.qe.skeleton.services.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@WithMockUser(roles = "EMPLOYEE")
class RaspberryPiClientControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MeasurementService measurementService;

    @MockitoBean
    private RaspberryPiService raspberryPiService;

    @MockitoBean
    private SensorStationService sensorStationService;

    @MockitoBean
    private PiConfigYamlBuilder piConfigYamlBuilder;

    @MockitoBean
    private ThresholdViolationService thresholdViolationService;

    @MockitoBean
    private TaskScheduler taskScheduler;

    @MockitoBean
    private EmailServiceImpl emailService;

    @MockitoBean
    private AvailableSensorStationCleaner availableSensorStationCleaner;

    @Test
    void receiveCurrentMeasurements_returns200AndDelegatesToService() throws Exception {
        RPMeasurementDTO dto = new RPMeasurementDTO(
                "2026-05-18T14:30:15.123",
                23.5F,
                45.0F,
                1013.2F,
                400.0F,
                10L,
                20L
        );

        mockMvc.perform(post("/api/cpi/5/measurements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(measurementService).saveMeasurementsFromRaspberryPi(5L, dto);
    }

    @Test
    void piBooted_returns200AndUpdatesPiInternally() throws Exception {
        RaspberryPiUpdateDTO dto = new RaspberryPiUpdateDTO(
                "192.168.1.20",
                "pi-office",
                DeviceStatus.ONLINE,
                null,
                null
        );

        mockMvc.perform(post("/api/cpi/5/booted")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(raspberryPiService).updateInternal(5L, dto);
    }

    @Test
    void getConfigYaml_returnsYamlFromBuilder() throws Exception {
        String yaml = """
                pi:
                  id: 5
                """;

        when(piConfigYamlBuilder.buildYaml(5L)).thenReturn(yaml);

        mockMvc.perform(get("/api/cpi/5/config"))
                .andExpect(status().isOk())
                .andExpect(content().string(yaml));

        verify(piConfigYamlBuilder).buildYaml(5L);
    }

    @Test
    void receiveAvailableSensorStations_returns200AddsStationsAndSchedulesCleanup() throws Exception {
        List<String> bleMacs = List.of("AA:BB:CC:DD:EE:FF", "11:22:33:44:55:66");

        mockMvc.perform(post("/api/cpi/5/discovered")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bleMacs)))
                .andExpect(status().isOk());

        verify(raspberryPiService).addAvailableSensorStations(5L, bleMacs);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(taskScheduler).schedule(runnableCaptor.capture(), instantCaptor.capture());

        assertThat(instantCaptor.getValue()).isAfter(Instant.now().plusSeconds(250));

        runnableCaptor.getValue().run();
        verify(availableSensorStationCleaner).cleanAvailableSensorStations(5L);
    }

    @Test
    void updateSensorStationStatus_returns200AndDelegatesToService() throws Exception {
        mockMvc.perform(patch("/api/cpi/5/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(DeviceStatus.ONLINE)))
                .andExpect(status().isOk());

        verify(sensorStationService).update(5L, 20L, DeviceStatus.ONLINE);
    }

    @Test
    void receiveActiveViolation_returns200AndCreatesViolation() throws Exception {
        ViolationActiveDTO dto = new ViolationActiveDTO(
                Metric.TEMPERATURE,
                10L,
                "2026-05-18T14:30:15.123",
                31.5F
        );

        mockMvc.perform(post("/api/cpi/5/violation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(thresholdViolationService).create(5L, dto);
    }

    @Test
    void deactivateActiveViolation_returns200AndResolvesViolation() throws Exception {
        ViolationResolvedDTO dto = new ViolationResolvedDTO(
                Metric.TEMPERATURE,
                10L,
                "2026-05-18T15:00:00.000"
        );

        mockMvc.perform(patch("/api/cpi/5/violation/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(thresholdViolationService).update(5L, dto);
    }
}
