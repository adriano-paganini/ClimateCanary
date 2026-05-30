package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.*;
import at.qe.skeleton.mappers.ClimateHintCreateMapper;
import at.qe.skeleton.mappers.ClimateHintMapper;
import at.qe.skeleton.models.ClimateHint;
import at.qe.skeleton.models.Metric;
import at.qe.skeleton.services.ClimateHintService;
import at.qe.skeleton.services.EmailServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@WithMockUser(roles = "BUILDING_ADMIN")
public class ClimateHintControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClimateHintService climateHintService;

    @MockitoBean
    private ClimateHintMapper climateHintMapper;

    @MockitoBean
    private ClimateHintCreateMapper climateHintCreateMapper;

    @MockitoBean
    private EmailServiceImpl emailService;

    private ObjectMapper objectMapper;

    private ClimateHint hint1;
    private ClimateHint hint2;
    private ClimateHintDTO dto1;
    private ClimateHintDTO dto2;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        hint1 = new ClimateHint();
        ReflectionTestUtils.setField(hint1, "id", 1L);

        hint2 = new ClimateHint();
        ReflectionTestUtils.setField(hint2, "id", 2L);

        dto1 = new ClimateHintDTO(
                1L,
                Metric.TEMPERATURE,
                "Open the window"
        );

        dto2 = new ClimateHintDTO(
                2L,
                Metric.HUMIDITY,
                "Use a dehumidifier"
        );
    }

    @Test
    void getAll_returns200() throws Exception {
        when(climateHintService.findAll()).thenReturn(List.of(hint1, hint2));
        when(climateHintMapper.mapTo(hint1)).thenReturn(dto1);
        when(climateHintMapper.mapTo(hint2)).thenReturn(dto2);

        mockMvc.perform(get("/api/climatehint"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));

        verify(climateHintService).findAll();
    }

    @Test
    void getById_returns200() throws Exception {
        when(climateHintService.getClimateHintById(1L)).thenReturn(hint1);
        when(climateHintMapper.mapTo(hint1)).thenReturn(dto1);

        mockMvc.perform(get("/api/climatehint/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(climateHintService).getClimateHintById(1L);
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(climateHintService.getClimateHintById(99L))
                .thenThrow(new NotFoundException("ClimateHint not found"));

        mockMvc.perform(get("/api/climatehint/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returns201() throws Exception {
        ClimateHintCreateDTO createDTO = new ClimateHintCreateDTO(
                Metric.TEMPERATURE,
                "Open window"
        );

        when(climateHintCreateMapper.mapFrom(any(ClimateHintCreateDTO.class)))
                .thenReturn(hint1);
        when(climateHintService.create(hint1)).thenReturn(hint1);
        when(climateHintMapper.mapTo(hint1)).thenReturn(dto1);

        mockMvc.perform(post("/api/climatehint")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/climatehint/1")))
                .andExpect(jsonPath("$.id", is(1)));

        verify(climateHintService).create(any(ClimateHint.class));
    }

    @Test
    void create_invalidPayload_returns400() throws Exception {
        mockMvc.perform(post("/api/climatehint")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(climateHintService);
    }

    @Test
    void update_returns200() throws Exception {
        ClimateHintUpdateDTO updateDTO = new ClimateHintUpdateDTO(
                Metric.HUMIDITY,
                "Updated hint"
        );

        when(climateHintService.update(eq(1L), any(ClimateHintUpdateDTO.class)))
                .thenReturn(hint1);
        when(climateHintMapper.mapTo(hint1)).thenReturn(dto1);

        mockMvc.perform(patch("/api/climatehint/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(climateHintService).update(eq(1L), any(ClimateHintUpdateDTO.class));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        ClimateHintUpdateDTO updateDTO = new ClimateHintUpdateDTO(
                Metric.HUMIDITY,
                "Updated hint"
        );

        when(climateHintService.update(eq(99L), any(ClimateHintUpdateDTO.class)))
                .thenThrow(new NotFoundException("ClimateHint not found"));

        mockMvc.perform(patch("/api/climatehint/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(climateHintService).delete(1L);

        mockMvc.perform(delete("/api/climatehint/1"))
                .andExpect(status().isNoContent());

        verify(climateHintService).delete(1L);
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(new NotFoundException("ClimateHint not found"))
                .when(climateHintService).delete(99L);

        mockMvc.perform(delete("/api/climatehint/99"))
                .andExpect(status().isNotFound());
    }
}