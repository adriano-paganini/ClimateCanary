package at.qe.skeleton.tests.controllers;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.AddressCreateDTO;
import at.qe.skeleton.dtos.AddressDTO;
import at.qe.skeleton.dtos.AddressUpdateDTO;
import at.qe.skeleton.mappers.AddressCreateMapper;
import at.qe.skeleton.mappers.AddressMapper;
import at.qe.skeleton.models.Address;
import at.qe.skeleton.services.AddressService;
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
@WithMockUser(authorities = "BUILDING_ADMIN")
class AddressControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AddressService addressService;

    @MockitoBean
    private AddressMapper addressMapper;

    @MockitoBean
    private AddressCreateMapper addressCreateMapper;

    @MockitoBean
    private EmailServiceImpl emailService;

    private ObjectMapper objectMapper;

    private Address address1;
    private Address address2;
    private AddressDTO addressDTO1;
    private AddressDTO addressDTO2;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        address1 = new Address();
        ReflectionTestUtils.setField(address1, "id", 1L);

        address2 = new Address();
        ReflectionTestUtils.setField(address2, "id", 2L);

        addressDTO1 = new AddressDTO(
                1L,
                "Austria",
                "6020",
                "Innsbruck",
                "Teststraße",
                "10",
                null
        );

        addressDTO2 = new AddressDTO(
                2L,
                "Austria",
                "1010",
                "Vienna",
                "Main Street",
                "5",
                "Top 2"
        );
    }

    @Test
    void getAll_returns200WithAllAddresses() throws Exception {
        when(addressService.getAll()).thenReturn(List.of(address1, address2));
        when(addressMapper.mapTo(address1)).thenReturn(addressDTO1);
        when(addressMapper.mapTo(address2)).thenReturn(addressDTO2);

        mockMvc.perform(get("/api/address").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));

        verify(addressService).getAll();
    }

    @Test
    void getById_exists_returns200() throws Exception {
        when(addressService.getById(1L)).thenReturn(address1);
        when(addressMapper.mapTo(address1)).thenReturn(addressDTO1);

        mockMvc.perform(get("/api/address/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(addressService).getById(1L);
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(addressService.getById(99L))
                .thenThrow(new NotFoundException("Address not found"));

        mockMvc.perform(get("/api/address/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_validPayload_returns201() throws Exception {
        AddressCreateDTO createDTO = new AddressCreateDTO(
                "Austria",
                "6020",
                "Innsbruck",
                "Teststraße",
                "10",
                null
        );

        when(addressCreateMapper.mapFrom(any(AddressCreateDTO.class))).thenReturn(address1);
        when(addressService.create(address1)).thenReturn(address1);
        when(addressMapper.mapTo(address1)).thenReturn(addressDTO1);

        mockMvc.perform(post("/api/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/address/1")))
                .andExpect(jsonPath("$.id", is(1)));

        verify(addressService).create(any(Address.class));
    }

    @Test
    void create_invalidPayload_returns400() throws Exception {
        mockMvc.perform(post("/api/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(addressService);
    }

    @Test
    void update_validPayload_returns200() throws Exception {
        AddressUpdateDTO updateDTO = new AddressUpdateDTO(
                "Austria",
                "6020",
                "Innsbruck",
                "Updated Street",
                "20",
                "Top 1"
        );

        when(addressService.update(eq(1L), any(AddressUpdateDTO.class)))
                .thenReturn(address1);
        when(addressMapper.mapTo(address1)).thenReturn(addressDTO1);

        mockMvc.perform(patch("/api/address/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(addressService).update(eq(1L), any(AddressUpdateDTO.class));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        AddressUpdateDTO updateDTO = new AddressUpdateDTO(
                "Austria",
                "6020",
                "Innsbruck",
                "Updated Street",
                "20",
                "Top 1"
        );

        when(addressService.update(eq(99L), any(AddressUpdateDTO.class)))
                .thenThrow(new NotFoundException("Address not found"));

        mockMvc.perform(patch("/api/address/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_exists_returns204() throws Exception {
        doNothing().when(addressService).delete(1L);

        mockMvc.perform(delete("/api/address/1"))
                .andExpect(status().isNoContent());

        verify(addressService).delete(1L);
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(new NotFoundException("Address not found"))
                .when(addressService).delete(99L);

        mockMvc.perform(delete("/api/address/99"))
                .andExpect(status().isNotFound());
    }
}