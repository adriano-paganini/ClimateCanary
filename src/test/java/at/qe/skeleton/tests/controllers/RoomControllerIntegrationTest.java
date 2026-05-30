package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.*;
import at.qe.skeleton.mappers.RoomCreateMapper;
import at.qe.skeleton.mappers.RoomMapper;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.models.RoomType;
import at.qe.skeleton.services.EmailServiceImpl;
import at.qe.skeleton.services.RoomService;
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
public class RoomControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private RoomMapper roomMapper;

    @MockitoBean
    private RoomCreateMapper roomCreateMapper;

    @MockitoBean
    private EmailServiceImpl emailService;

    private ObjectMapper objectMapper;

    private Room room1;
    private Room room2;

    private RoomDTO dto1;
    private RoomDTO dto2;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        room1 = new Room();
        ReflectionTestUtils.setField(room1, "id", 1L);

        room2 = new Room();
        ReflectionTestUtils.setField(room2, "id", 2L);

        dto1 = new RoomDTO(
                1L,
                "Room A",
                RoomType.OFFICE,
                true,
                1L,
                null,
                true
        );

        dto2 = new RoomDTO(
                2L,
                "Room B",
                RoomType.COMMON_AREAS,
                true,
                1L,
                null,
                false
        );
    }

    @Test
    void getAll_returns200WithRooms() throws Exception {
        when(roomService.getAll()).thenReturn(List.of(room1, room2));
        when(roomMapper.mapTo(room1)).thenReturn(dto1);
        when(roomMapper.mapTo(room2)).thenReturn(dto2);

        mockMvc.perform(get("/api/room"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));

        verify(roomService).getAll();
    }

    @Test
    void getById_exists_returns200() throws Exception {
        when(roomService.getById(1L)).thenReturn(room1);
        when(roomMapper.mapTo(room1)).thenReturn(dto1);

        mockMvc.perform(get("/api/room/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(roomService).getById(1L);
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(roomService.getById(99L))
                .thenThrow(new NotFoundException("Room not found"));

        mockMvc.perform(get("/api/room/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_validPayload_returns201() throws Exception {
        RoomCreateDTO createDTO = new RoomCreateDTO(
                "Room A",
                RoomType.OFFICE,
                false,
                1L,
                1L
        );

        when(roomCreateMapper.mapFrom(any())).thenReturn(room1);
        when(roomService.create(room1)).thenReturn(room1);
        when(roomMapper.mapTo(room1)).thenReturn(dto1);

        mockMvc.perform(post("/api/room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/room/1")))
                .andExpect(jsonPath("$.id", is(1)));

        verify(roomService).create(any());
    }

    @Test
    void create_invalidPayload_returns400() throws Exception {
        mockMvc.perform(post("/api/room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(roomService);
    }

    @Test
    void update_validPayload_returns200() throws Exception {
        RoomUpdateDTO updateDTO = new RoomUpdateDTO(
                "Updated",
                null,
                null,
                null,
                null
        );

        when(roomService.update(eq(1L), any())).thenReturn(room1);
        when(roomMapper.mapTo(room1)).thenReturn(dto1);

        mockMvc.perform(patch("/api/room/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(roomService).update(eq(1L), any());
    }

    @Test
    void update_notFound_returns404() throws Exception {
        when(roomService.update(eq(99L), any()))
                .thenThrow(new NotFoundException("Room not found"));

        mockMvc.perform(patch("/api/room/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RoomUpdateDTO(null, null, null, null, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_exists_returns204() throws Exception {
        doNothing().when(roomService).delete(1L);

        mockMvc.perform(delete("/api/room/1"))
                .andExpect(status().isNoContent());

        verify(roomService).delete(1L);
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(new NotFoundException("Room not found"))
                .when(roomService).delete(99L);

        mockMvc.perform(delete("/api/room/99"))
                .andExpect(status().isNotFound());
    }
}