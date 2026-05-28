package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.ThresholdViolationCreateDTO;
import at.qe.skeleton.dtos.ThresholdViolationDTO;
import at.qe.skeleton.dtos.ThresholdViolationUpdateDTO;
import at.qe.skeleton.mappers.ThresholdViolationMapper;
import at.qe.skeleton.models.Metric;
import at.qe.skeleton.models.ThresholdViolation;
import at.qe.skeleton.models.ViolationStatus;
import at.qe.skeleton.services.ThresholdViolationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@WithMockUser(authorities = "SYSTEM_ADMIN")
class ThresholdViolationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ThresholdViolationService thresholdViolationService;

    @MockitoBean
    private ThresholdViolationMapper thresholdViolationMapper;

    private ObjectMapper objectMapper;

    private ThresholdViolation v1;
    private ThresholdViolation v2;
    private ThresholdViolationDTO dto1;
    private ThresholdViolationDTO dto2;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        v1 = new ThresholdViolation();
        ReflectionTestUtils.setField(v1, "id", 1L);

        v2 = new ThresholdViolation();
        ReflectionTestUtils.setField(v2, "id", 2L);

        dto1 = new ThresholdViolationDTO(
                1L,
                Metric.TEMPERATURE,
                25.5f,
                ViolationStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(2),
                10L,
                20L,
                null
        );

        dto2 = new ThresholdViolationDTO(
                2L,
                Metric.TEMPERATURE,
                65.5f,
                ViolationStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(5),
                30L,
                20L,
                null
        );
    }

    @Test
    void getAll_noFilter_returns200WithAll() throws Exception {
        when(thresholdViolationService.findAll(null, null, null))
                .thenReturn(List.of(v1, v2));

        when(thresholdViolationMapper.mapToListItem(v1)).thenReturn(dto1);
        when(thresholdViolationMapper.mapToListItem(v2)).thenReturn(dto2);

        mockMvc.perform(get("/api/thresholdviolation")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));

        verify(thresholdViolationService).findAll(null, null, null);
    }

    @Test
    void getAll_filterByStatus_returns200() throws Exception {
        when(thresholdViolationService.findAll(ViolationStatus.ACTIVE, null, null))
                .thenReturn(List.of(v1));

        when(thresholdViolationMapper.mapToListItem(v1)).thenReturn(dto1);

        mockMvc.perform(get("/api/thresholdviolation")
                        .param("violationStatus", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)));

        verify(thresholdViolationService)
                .findAll(ViolationStatus.ACTIVE, null, null);
    }

    @Test
    void getById_exists_returns200() throws Exception {
        when(thresholdViolationService.findById(1L)).thenReturn(v1);
        when(thresholdViolationMapper.mapTo(v1)).thenReturn(dto1);

        mockMvc.perform(get("/api/thresholdviolation/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(thresholdViolationService).findById(1L);
    }

    @Test
    void create_valid_returns200() throws Exception {

        ThresholdViolationCreateDTO createDTO = new ThresholdViolationCreateDTO(
                Metric.TEMPERATURE,
                25.5f,
                LocalDateTime.now(),
                1L,
                1L,
                List.of()
        );

        when(thresholdViolationService.create(any(ThresholdViolationCreateDTO.class)))
                .thenReturn(v1);

        when(thresholdViolationMapper.mapTo(v1))
                .thenReturn(dto1);

        mockMvc.perform(post("/api/thresholdviolation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(thresholdViolationService)
                .create(any(ThresholdViolationCreateDTO.class));
    }

    @Test
    void update_valid_returns200() throws Exception {
        ThresholdViolationUpdateDTO updateDTO = mock(ThresholdViolationUpdateDTO.class);

        when(thresholdViolationService.update(eq(1L), (ThresholdViolationUpdateDTO) any())).thenReturn(v1);
        when(thresholdViolationMapper.mapTo(v1)).thenReturn(dto1);

        mockMvc.perform(patch("/api/thresholdviolation/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(thresholdViolationService).update(eq(1L), (ThresholdViolationUpdateDTO) any());
    }

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(thresholdViolationService).delete(1L);

        mockMvc.perform(delete("/api/thresholdviolation/1"))
                .andExpect(status().isNoContent());

        verify(thresholdViolationService).delete(1L);
    }
}
