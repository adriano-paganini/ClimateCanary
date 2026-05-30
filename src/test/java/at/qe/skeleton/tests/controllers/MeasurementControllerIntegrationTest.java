package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.MeasurementDTO;
import at.qe.skeleton.mappers.MeasurementMapper;
import at.qe.skeleton.models.Measurement;
import at.qe.skeleton.models.Metric;
import at.qe.skeleton.services.EmailServiceImpl;
import at.qe.skeleton.services.MeasurementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@WithMockUser(roles = "EMPLOYEE")
public class MeasurementControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeasurementService measurementService;

    @MockitoBean
    private MeasurementMapper measurementMapper;

    @MockitoBean
    private EmailServiceImpl emailService;

    private Measurement m1;
    private Measurement m2;

    private MeasurementDTO dto1;
    private MeasurementDTO dto2;

    @BeforeEach
    void setUp() {
        m1 = new Measurement();
        ReflectionTestUtils.setField(m1, "id", 1L);

        m2 = new Measurement();
        ReflectionTestUtils.setField(m2, "id", 2L);

        dto1 = new MeasurementDTO(
                1L,
                LocalDateTime.now(),
                22.5F,
                Metric.TEMPERATURE,
                10L,
                null,
                null
        );

        dto2 = new MeasurementDTO(
                2L,
                LocalDateTime.now(),
                55.0F,
                Metric.HUMIDITY,
                10L,
                null,
                null
        );
    }

    @Test
    void getAll_noFilter_returns200() throws Exception {
        when(measurementService.getFiltered(null, null, null, null))
                .thenReturn(List.of(m1, m2));

        when(measurementMapper.mapTo(m1)).thenReturn(dto1);
        when(measurementMapper.mapTo(m2)).thenReturn(dto2);

        mockMvc.perform(get("/api/measurement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));
    }

    @Test
    void getAll_filterByRoom_returns200() throws Exception {
        when(measurementService.getFiltered(eq(10L), isNull(), isNull(), isNull()))
                .thenReturn(List.of(m1));

        when(measurementMapper.mapTo(m1)).thenReturn(dto1);

        mockMvc.perform(get("/api/measurement")
                        .param("roomId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomId", is(10)));
    }

    @Test
    void getAll_filterByMetric_returns200() throws Exception {
        when(measurementService.getFiltered(isNull(), eq(Metric.TEMPERATURE), isNull(), isNull())).thenReturn(List.of(m1));

        when(measurementMapper.mapTo(m1)).thenReturn(dto1);

        mockMvc.perform(get("/api/measurement")
                        .param("metric", "TEMPERATURE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].metric", is("TEMPERATURE")));
    }

    @Test
    void getAll_filterByDateRange_returns200() throws Exception {
        String from = "2024-01-01T00:00:00";
        String to = "2024-12-31T23:59:59";

        when(measurementService.getFiltered(any(), any(), any(), any()))
                .thenReturn(List.of(m1));

        when(measurementMapper.mapTo(m1)).thenReturn(dto1);

        mockMvc.perform(get("/api/measurement")
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getAll_allFiltersCombined_returns200() throws Exception {
        when(measurementService.getFiltered(
                eq(10L),
                eq(Metric.TEMPERATURE),
                any(),
                any()))
                .thenReturn(List.of(m1));

        when(measurementMapper.mapTo(m1)).thenReturn(dto1);

        mockMvc.perform(get("/api/measurement")
                        .param("roomId", "10")
                        .param("metric", "TEMPERATURE")
                        .param("from", "2024-01-01T00:00:00")
                        .param("to", "2024-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(1)));
    }

    @Test
    void getById_exists_returns200() throws Exception {
        when(measurementService.getById(1L)).thenReturn(m1);
        when(measurementMapper.mapTo(m1)).thenReturn(dto1);

        mockMvc.perform(get("/api/measurement/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(measurementService.getById(99L))
                .thenThrow(new NotFoundException("Measurement not found"));

        mockMvc.perform(get("/api/measurement/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getLatestPerMetric_returns200WithMap() throws Exception {
        Map<Metric, Measurement> map = Map.of(
                Metric.TEMPERATURE, m1,
                Metric.HUMIDITY, m2
        );

        when(measurementService.getLatestPerMetric(10L)).thenReturn(map);
        when(measurementMapper.mapTo(m1)).thenReturn(dto1);
        when(measurementMapper.mapTo(m2)).thenReturn(dto2);

        mockMvc.perform(get("/api/measurement/room/10/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TEMPERATURE.id", is(1)))
                .andExpect(jsonPath("$.HUMIDITY.id", is(2)));
    }

    @Test
    void getLatestPerMetric_notFound_returns404() throws Exception {
        when(measurementService.getLatestPerMetric(99L))
                .thenThrow(new NotFoundException("Room not found"));

        mockMvc.perform(get("/api/measurement/room/99/latest"))
                .andExpect(status().isNotFound());
    }
}