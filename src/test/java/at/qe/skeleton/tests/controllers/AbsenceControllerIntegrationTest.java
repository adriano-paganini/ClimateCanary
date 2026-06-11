package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.AbsenceCreateDTO;
import at.qe.skeleton.dtos.AbsenceDTO;
import at.qe.skeleton.dtos.AbsenceUpdateDTO;
import at.qe.skeleton.mappers.AbsenceCreateMapper;
import at.qe.skeleton.mappers.AbsenceMapper;
import at.qe.skeleton.models.Absence;
import at.qe.skeleton.models.AbsenceStatus;
import at.qe.skeleton.models.AbsenceType;
import at.qe.skeleton.services.AbsenceService;
import at.qe.skeleton.services.EmailServiceImpl;
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
@WithMockUser(authorities = "EMPLOYEE")
class AbsenceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AbsenceService absenceService;

    @MockitoBean
    private AbsenceMapper absenceMapper;

    @MockitoBean
    private AbsenceCreateMapper absenceCreateMapper;

    @MockitoBean
    private EmailServiceImpl emailService;

    private ObjectMapper objectMapper;

    private Absence absence1;
    private Absence absence2;
    private AbsenceDTO absenceDTO1;
    private AbsenceDTO absenceDTO2;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        absence1 = new Absence();
        ReflectionTestUtils.setField(absence1, "id", 1L);
        absence2 = new Absence();
        ReflectionTestUtils.setField(absence2, "id", 2L);

        absenceDTO1 = new AbsenceDTO(
                1L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AbsenceType.SICKNESS,
                AbsenceStatus.PLANNED,
                10L,
                "user10",
                "User",
                "Ten"
        );

        absenceDTO2 = new AbsenceDTO(
                2L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                AbsenceType.HOLIDAY,
                AbsenceStatus.PLANNED,
                20L,
                "user20",
                "User",
                "Twenty"
        );

    }

    @Test
    void getAll_noFilter_returns200WithAllAbsences() throws Exception {
        when(absenceService.getAll(null, null)).thenReturn(List.of(absence1, absence2));
        when(absenceMapper.mapTo(absence1)).thenReturn(absenceDTO1);
        when(absenceMapper.mapTo(absence2)).thenReturn(absenceDTO2);

        mockMvc.perform(get("/api/absence").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));

        verify(absenceService).getAll(null, null);
    }

    @Test
    void getAll_filterByUserxId_returns200WithFilteredAbsences() throws Exception {
        when(absenceService.getAll(10L, null)).thenReturn(List.of(absence1));
        when(absenceMapper.mapTo(absence1)).thenReturn(absenceDTO1);

        mockMvc.perform(get("/api/absence")
                        .param("userxId", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userxId", is(10)));

        verify(absenceService).getAll(10L, null);
    }

    @Test
    void getAll_filterByDepartmentId_returns200WithFilteredAbsences() throws Exception {
        when(absenceService.getAll(null, 100L)).thenReturn(List.of(absence1));
        when(absenceMapper.mapTo(absence1)).thenReturn(absenceDTO1);

        mockMvc.perform(get("/api/absence")
                        .param("departmentId", "100")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(absenceService).getAll(null, 100L);
    }

    @Test
    void getAll_filterByBothParams_returns200WithFilteredAbsences() throws Exception {
        when(absenceService.getAll(10L, 100L)).thenReturn(List.of(absence1));
        when(absenceMapper.mapTo(absence1)).thenReturn(absenceDTO1);

        mockMvc.perform(get("/api/absence")
                        .param("userxId", "10")
                        .param("departmentId", "100")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(absenceService).getAll(10L, 100L);
    }

    @Test
    void getAll_noAbsencesExist_returns200WithEmptyList() throws Exception {
        when(absenceService.getAll(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/absence").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getById_exists_returns200WithAbsence() throws Exception {
        when(absenceService.getById(1L)).thenReturn(absence1);
        when(absenceMapper.mapTo(absence1)).thenReturn(absenceDTO1);

        mockMvc.perform(get("/api/absence/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.userxId", is(10)));

        verify(absenceService).getById(1L);
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(absenceService.getById(99L))
                .thenThrow(new NotFoundException("Absence not found"));

        mockMvc.perform(get("/api/absence/99").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_validPayload_returns201WithLocationHeaderAndBody() throws Exception {
        AbsenceCreateDTO createDTO = new AbsenceCreateDTO(
                LocalDateTime.of(2024, 3, 1, 12, 0),
                LocalDateTime.of(2024, 3, 5, 12, 0),
                AbsenceType.SICKNESS,
                10L
        );

        when(absenceCreateMapper.mapFrom(any(AbsenceCreateDTO.class))).thenReturn(absence1);
        when(absenceService.create(absence1)).thenReturn(absence1);
        when(absenceMapper.mapTo(absence1)).thenReturn(absenceDTO1);

        mockMvc.perform(post("/api/absence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/absence/1")))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.userxId", is(10)));

        verify(absenceService).create(any(Absence.class));
    }

    @Test
    void create_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/api/absence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(absenceService);
    }

    @Test
    void update_validPayload_returns200WithUpdatedAbsence() throws Exception {
        AbsenceUpdateDTO updateDTO = new AbsenceUpdateDTO(
                LocalDateTime.of(2024, 4, 1, 12, 0),
                LocalDateTime.of(2024, 4, 3, 12, 0),
                null,
                null,
                null
        );

        when(absenceService.update(eq(1L), any(AbsenceUpdateDTO.class))).thenReturn(absence1);
        when(absenceMapper.mapTo(absence1)).thenReturn(absenceDTO1);

        mockMvc.perform(patch("/api/absence/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(absenceService).update(eq(1L), any(AbsenceUpdateDTO.class));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        AbsenceUpdateDTO updateDTO = new AbsenceUpdateDTO(
                LocalDateTime.of(2024, 4, 1, 12, 0),
                LocalDateTime.of(2024, 4, 3, 12, 0),
                null,
                null,
                null
        );

        when(absenceService.update(eq(99L), any(AbsenceUpdateDTO.class)))
                .thenThrow(new NotFoundException("Absence not found"));

        mockMvc.perform(patch("/api/absence/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_exists_returns204WithNoBody() throws Exception {
        doNothing().when(absenceService).delete(1L);

        mockMvc.perform(delete("/api/absence/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(is(emptyOrNullString())));

        verify(absenceService, times(1)).delete(1L);
        verifyNoMoreInteractions(absenceService);
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(new NotFoundException("Absence not found"))
                .when(absenceService).delete(99L);

        mockMvc.perform(delete("/api/absence/99"))
                .andExpect(status().isNotFound());
    }
}
