package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.dtos.EmployeeProfileDTO;
import at.qe.skeleton.mappers.EmployeeProfileCreateMapper;
import at.qe.skeleton.mappers.EmployeeProfileMapper;
import at.qe.skeleton.models.EmployeeProfile;
import at.qe.skeleton.services.EmailServiceImpl;
import at.qe.skeleton.services.EmployeeProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@WithMockUser(roles = "EMPLOYEE")
public class EmployeeProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeProfileService service;

    @MockitoBean
    private EmployeeProfileMapper mapper;

    @MockitoBean
    private EmployeeProfileCreateMapper createMapper;

    @MockitoBean
    private EmailServiceImpl emailService;

    private EmployeeProfile profile;
    private EmployeeProfileDTO dto;

    @BeforeEach
    void setUp() {
        profile = new EmployeeProfile();
        ReflectionTestUtils.setField(profile, "id", 1L);

        dto = new EmployeeProfileDTO(1L,10L,null,null, null, null);
    }

    @Test
    void getMe_noProfile_returns204() throws Exception {
        when(service.getMyProfile()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/employeeprofile/me"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getMe_exists_returns200() throws Exception {
        when(service.getMyProfile()).thenReturn(Optional.of(profile));
        when(mapper.mapTo(profile)).thenReturn(dto);

        mockMvc.perform(get("/api/employeeprofile/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }
}
