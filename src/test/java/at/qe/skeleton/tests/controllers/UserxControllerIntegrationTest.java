package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.AbsenceDTO;
import at.qe.skeleton.dtos.UserxDTO;
import at.qe.skeleton.dtos.UserxSelfUpdateDTO;
import at.qe.skeleton.mappers.AbsenceMapper;
import at.qe.skeleton.mappers.UserxMapper;
import at.qe.skeleton.models.Absence;
import at.qe.skeleton.models.Userx;
import at.qe.skeleton.services.AbsenceService;
import at.qe.skeleton.services.AuthenticatedUserService;
import at.qe.skeleton.services.UserxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@WithMockUser(username = "user1", roles = "EMPLOYEE")
class UserxControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserxMapper userxMapper;

    @MockitoBean
    private AuthenticatedUserService authenticatedUserService;

    @MockitoBean
    private UserxService userxService;

    @MockitoBean
    private AbsenceMapper absenceMapper;

    @MockitoBean
    private AbsenceService absenceService;

    @Autowired
    private ObjectMapper objectMapper;

    private Userx user;
    private UserxDTO userDTO;

    private Absence absence;
    private AbsenceDTO absenceDTO;

    @BeforeEach
    void setUp() {
        user = new Userx();
        absence = new Absence();

        userDTO = mock(UserxDTO.class);
        absenceDTO = mock(AbsenceDTO.class);
    }

    @Test
    void getCurrentUser_returns200() throws Exception {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
        when(userxMapper.mapTo(user)).thenReturn(userDTO);

        mockMvc.perform(get("/api/userx/me"))
                .andExpect(status().isOk());

        verify(authenticatedUserService).getAuthenticatedUser();
    }

    @Test
    void isAuthenticated_userPresent_returns200() throws Exception {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("user1");

        mockMvc.perform(get("/api/userx/authenticated")
                        .requestAttr("userDetails", userDetails))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void isAuthenticated_noUser_returns401() throws Exception {
        mockMvc.perform(get("/api/userx/authenticated"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateCurrentUser_returns200() throws Exception {

        UserxSelfUpdateDTO dto = new UserxSelfUpdateDTO(
                "John",
                "Doe",
                "john@test.com",
                "123"
        );

        when(userxService.saveCurrentUser(any())).thenReturn(user);
        when(userxMapper.mapTo(user)).thenReturn(userDTO);

        mockMvc.perform(patch("/api/userx/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(userxService).saveCurrentUser(any());
    }

    @Test
    void deleteCurrentUser_returns204() throws Exception {

        doNothing().when(userxService).deleteCurrentUser();

        mockMvc.perform(delete("/api/userx/me"))
                .andExpect(status().isNoContent());

        verify(userxService).deleteCurrentUser();
    }

    @Test
    void getCurrentUserAbsences_returns200() throws Exception {

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
        when(absenceService.getAbsencesForUser(user)).thenReturn(List.of(absence));
        when(absenceMapper.mapTo(absence)).thenReturn(absenceDTO);

        mockMvc.perform(get("/api/userx/me/absences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(absenceService).getAbsencesForUser(user);
    }
}