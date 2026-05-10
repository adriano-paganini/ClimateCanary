package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.*;
import at.qe.skeleton.mappers.*;
import at.qe.skeleton.models.*;
import at.qe.skeleton.services.DepartmentService;
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
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@WithMockUser(roles = "EMPLOYEE")
public class DepartmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DepartmentService departmentService;

    @MockitoBean
    private DepartmentMapper departmentMapper;

    @MockitoBean
    private DepartmentCreateMapper departmentCreateMapper;

    @MockitoBean
    private UserxMapper userxMapper;

    @MockitoBean
    private RoomMapper roomMapper;

    private ObjectMapper objectMapper;

    private Department department;
    private DepartmentDTO departmentDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        department = new Department();
        ReflectionTestUtils.setField(department, "id", 1L);

        departmentDTO = new DepartmentDTO(
                1L,
                "IT",
                List.of(10L),
                100L
        );
    }

    @Test
    void getAll_returns200() throws Exception {
        when(departmentService.getAll()).thenReturn(List.of(department));
        when(departmentMapper.mapTo(department)).thenReturn(departmentDTO);

        mockMvc.perform(get("/api/department"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)));

        verify(departmentService).getAll();
    }

    @Test
    void getById_returns200() throws Exception {
        when(departmentService.getDepartmentById(1L)).thenReturn(department);
        when(departmentMapper.mapTo(department)).thenReturn(departmentDTO);

        mockMvc.perform(get("/api/department/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(departmentService).getDepartmentById(1L);
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(departmentService.getDepartmentById(99L)).thenThrow(new NotFoundException("Department not found"));

        mockMvc.perform(get("/api/department/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRooms_returns200() throws Exception {
        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 10L);
        department.setRooms(List.of(room));

        RoomDTO roomDTO = new RoomDTO(10L, "Room", null, null, null, null, true);

        when(departmentService.getDepartmentById(1L)).thenReturn(department);
        when(roomMapper.mapTo(room)).thenReturn(roomDTO);

        mockMvc.perform(get("/api/department/1/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(10)));

        verify(departmentService).getDepartmentById(1L);
    }

    @Test
    void getLeader_returns200() throws Exception {
        Userx user = new Userx();
        ReflectionTestUtils.setField(user, "id", 100L);

        department.setDepartmentLeader(user);

        UserxDTO userDTO = new UserxDTO(
                100L,
                null,
                null,
                null,
                null,
                "user",
                "John",
                "Doe",
                "john@example.com",
                null,
                true,
                Set.of()
        );

        when(departmentService.getDepartmentById(1L)).thenReturn(department);
        when(userxMapper.mapTo(user)).thenReturn(userDTO);

        mockMvc.perform(get("/api/department/1/leader"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(100)));

        verify(departmentService).getDepartmentById(1L);
    }

    @Test
    void create_returns201() throws Exception {
        DepartmentCreateDTO createDTO = new DepartmentCreateDTO(
                "IT",
                List.of(10L),
                100L
        );

        when(departmentCreateMapper.mapFrom(any(DepartmentCreateDTO.class))).thenReturn(department);
        when(departmentService.create(department)).thenReturn(department);
        when(departmentMapper.mapTo(department)).thenReturn(departmentDTO);

        mockMvc.perform(post("/api/department")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/department/1")))
                .andExpect(jsonPath("$.id", is(1)));

        verify(departmentService).create(any(Department.class));
    }

    @Test
    void create_invalidPayload_returns400() throws Exception {
        mockMvc.perform(post("/api/department")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(departmentService);
    }

    @Test
    void update_returns200() throws Exception {
        DepartmentUpdateDTO updateDTO = new DepartmentUpdateDTO(
                "Updated",
                List.of(10L),
                100L
        );

        when(departmentService.update(eq(1L), any(DepartmentUpdateDTO.class))).thenReturn(department);
        when(departmentMapper.mapTo(department)).thenReturn(departmentDTO);

        mockMvc.perform(patch("/api/department/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(departmentService).update(eq(1L), any(DepartmentUpdateDTO.class));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        DepartmentUpdateDTO updateDTO = new DepartmentUpdateDTO(
                "Updated",
                List.of(10L),
                100L
        );

        when(departmentService.update(eq(99L), any(DepartmentUpdateDTO.class)))
                .thenThrow(new NotFoundException("Department not found"));

        mockMvc.perform(patch("/api/department/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(departmentService).delete(1L);

        mockMvc.perform(delete("/api/department/1"))

                .andExpect(status().isNoContent());

        verify(departmentService).delete(1L);
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(new NotFoundException("Department not found"))
                .when(departmentService).delete(99L);

        mockMvc.perform(delete("/api/department/99"))
                .andExpect(status().isNotFound());
    }
}