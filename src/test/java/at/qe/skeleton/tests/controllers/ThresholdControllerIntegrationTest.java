package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.*;
import at.qe.skeleton.mappers.ThresholdMapper;
import at.qe.skeleton.models.Metric;
import at.qe.skeleton.models.Threshold;
import at.qe.skeleton.models.ThresholdType;
import at.qe.skeleton.services.ThresholdService;
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
public class ThresholdControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ThresholdService thresholdService;

    @MockitoBean
    private ThresholdMapper thresholdMapper;

    private ObjectMapper objectMapper;

    private Threshold threshold1;
    private Threshold threshold2;

    private ThresholdDTO dto1;
    private ThresholdDTO dto2;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        threshold1 = new Threshold();
        ReflectionTestUtils.setField(threshold1, "id", 1L);

        threshold2 = new Threshold();
        ReflectionTestUtils.setField(threshold2, "id", 2L);

        dto1 = new ThresholdDTO(
                1L,
                1L,
                Metric.TEMPERATURE,
                25.0F,
                ThresholdType.UPPER,
                null,
                true
        );

        dto2 = new ThresholdDTO(
                2L,
                2L,
                Metric.HUMIDITY,
                60.0F,
                ThresholdType.LOWER,
                null,
                true
        );
    }

    @Test
    void getAll_noFilter_returns200() throws Exception {
        when(thresholdService.getAll(null, null)).thenReturn(List.of(threshold1, threshold2));
        when(thresholdMapper.mapTo(threshold1)).thenReturn(dto1);
        when(thresholdMapper.mapTo(threshold2)).thenReturn(dto2);

        mockMvc.perform(get("/api/threshold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        verify(thresholdService).getAll(null, null);
    }

    @Test
    void getAll_filterByRoomId_returns200() throws Exception {
        when(thresholdService.getAll(eq(1L), isNull())).thenReturn(List.of(threshold1));
        when(thresholdMapper.mapTo(threshold1)).thenReturn(dto1);

        mockMvc.perform(get("/api/threshold")
                        .param("roomId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(thresholdService).getAll(eq(1L), isNull());
    }

    @Test
    void getAll_filterByMetric_returns200() throws Exception {
        when(thresholdService.getAll(isNull(), eq(Metric.TEMPERATURE)))
                .thenReturn(List.of(threshold1));
        when(thresholdMapper.mapTo(threshold1)).thenReturn(dto1);

        mockMvc.perform(get("/api/threshold")
                        .param("metric", "TEMPERATURE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].metric", is("TEMPERATURE")));
    }

    @Test
    void getById_exists_returns200() throws Exception {
        when(thresholdService.getThresholdById(1L)).thenReturn(threshold1);
        when(thresholdMapper.mapTo(threshold1)).thenReturn(dto1);

        mockMvc.perform(get("/api/threshold/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(thresholdService).getThresholdById(1L);
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(thresholdService.getThresholdById(99L))
                .thenThrow(new NotFoundException("Threshold not found"));

        mockMvc.perform(get("/api/threshold/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_validPayload_returns200() throws Exception {
        ThresholdCreateDTO createDTO = new ThresholdCreateDTO(
                1L,
                Metric.TEMPERATURE,
                25.0F,
                ThresholdType.UPPER,
                null
        );

        when(thresholdService.create(any())).thenReturn(threshold1);
        when(thresholdMapper.mapTo(threshold1)).thenReturn(dto1);

        mockMvc.perform(post("/api/threshold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(thresholdService).create(any());
    }

    @Test
    void create_invalidPayload_returns400() throws Exception {
        mockMvc.perform(post("/api/threshold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(thresholdService);
    }

    @Test
    void update_validPayload_returns200() throws Exception {
        ThresholdUpdateDTO updateDTO = new ThresholdUpdateDTO(
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(thresholdService.update(eq(1L), any()))
                .thenReturn(threshold1);
        when(thresholdMapper.mapTo(threshold1)).thenReturn(dto1);

        mockMvc.perform(patch("/api/threshold/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(thresholdService).update(eq(1L), any());
    }

    @Test
    void update_notFound_returns404() throws Exception {
        when(thresholdService.update(eq(99L), any()))
                .thenThrow(new NotFoundException("Threshold not found"));

        mockMvc.perform(patch("/api/threshold/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ThresholdUpdateDTO(
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_exists_returns204() throws Exception {
        doNothing().when(thresholdService).delete(1L);

        mockMvc.perform(delete("/api/threshold/1"))
                .andExpect(status().isNoContent());

        verify(thresholdService).delete(1L);
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(new NotFoundException("Threshold not found"))
                .when(thresholdService).delete(99L);

        mockMvc.perform(delete("/api/threshold/99"))
                .andExpect(status().isNotFound());
    }
}