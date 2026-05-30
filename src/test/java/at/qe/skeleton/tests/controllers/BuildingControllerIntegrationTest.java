package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.*;
import at.qe.skeleton.mappers.*;
import at.qe.skeleton.models.*;
import at.qe.skeleton.services.BuildingService;
import at.qe.skeleton.services.EmailServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@WithMockUser(roles = "EMPLOYEE")
public class BuildingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BuildingService buildingService;

    @MockitoBean
    private BuildingMapper buildingMapper;

    @MockitoBean
    private BuildingCreateMapper buildingCreateMapper;

    @MockitoBean
    private RoomMapper roomMapper;

    @MockitoBean
    private EmailServiceImpl emailService;

    private ObjectMapper objectMapper;

    private Building building1;
    private Building building2;
    private BuildingDTO buildingDTO1;
    private BuildingDTO buildingDTO2;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        building1 = new Building();
        ReflectionTestUtils.setField(building1, "id", 1L);

        building2 = new Building();
        ReflectionTestUtils.setField(building2, "id", 2L);

        buildingDTO1 = new BuildingDTO(1L, "Building A", 10L);
        buildingDTO2 = new BuildingDTO(2L, "Building B", 20L);
    }

    @Test
    void getAll_returns200() throws Exception {
        when(buildingService.getAllBuildings()).thenReturn(List.of(building1, building2));
        when(buildingMapper.mapTo(building1)).thenReturn(buildingDTO1);
        when(buildingMapper.mapTo(building2)).thenReturn(buildingDTO2);

        mockMvc.perform(get("/api/building"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));
        verify(buildingService).getAllBuildings();
    }

    @Test
    void getById_returns200() throws Exception {
        when(buildingService.getBuildingById(1L)).thenReturn(building1);
        when(buildingMapper.mapTo(building1)).thenReturn(buildingDTO1);

        mockMvc.perform(get("/api/building/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(buildingService).getBuildingById(1L);
    }

    @Test
    void getById_notFound_returns404() throws Exception {

        when(buildingService.getBuildingById(99L))
                .thenThrow(new NotFoundException("Building not found"));

        mockMvc.perform(get("/api/building/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returns201() throws Exception {
        BuildingCreateDTO createDTO = new BuildingCreateDTO("New Building", 10L);

        when(buildingCreateMapper.mapFrom(any(BuildingCreateDTO.class)))
                .thenReturn(building1);
        when(buildingService.create(building1)).thenReturn(building1);
        when(buildingMapper.mapTo(building1)).thenReturn(buildingDTO1);

        mockMvc.perform(post("/api/building")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/building/1")))
                .andExpect(jsonPath("$.id", is(1)));

        verify(buildingService).create(any(Building.class));
    }

    @Test
    void update_returns200() throws Exception {
        BuildingUpdateDTO updateDTO = new BuildingUpdateDTO("Updated", 10L);

        when(buildingService.update(eq(1L), any(BuildingUpdateDTO.class))).thenReturn(building1);
        when(buildingMapper.mapTo(building1)).thenReturn(buildingDTO1);

        mockMvc.perform(patch("/api/building/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(buildingService).update(eq(1L), any(BuildingUpdateDTO.class));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        BuildingUpdateDTO updateDTO = new BuildingUpdateDTO("Updated", 10L);

        when(buildingService.update(eq(99L), any(BuildingUpdateDTO.class)))
                .thenThrow(new NotFoundException("Building not found"));

        mockMvc.perform(patch("/api/building/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(buildingService).delete(1L);
        mockMvc.perform(delete("/api/building/1")).andExpect(status().isNoContent());
        verify(buildingService).delete(1L);
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(new NotFoundException("Building not found")).when(buildingService).delete(99L);
        mockMvc.perform(delete("/api/building/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRooms_returns200() throws Exception {
        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 10L);

        building1.setRooms(List.of(room));

        RoomDTO roomDTO = new RoomDTO(10L, "Room", null, null, null, null, true);

        when(buildingService.getBuildingById(1L)).thenReturn(building1);
        when(roomMapper.mapTo(room)).thenReturn(roomDTO);

        mockMvc.perform(get("/api/building/1/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(10)));

        verify(buildingService).getBuildingById(1L);
    }
}