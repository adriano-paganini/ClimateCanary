package at.qe.skeleton.tests.services;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.SensorStationUpdateDTO;
import at.qe.skeleton.models.*;
import at.qe.skeleton.repositories.SensorStationRepository;
import at.qe.skeleton.services.RaspberryPiService;
import at.qe.skeleton.services.RoomService;
import at.qe.skeleton.services.SensorStationService;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class SensorStationServiceTest {

    @Mock
    private SensorStationRepository repo;

    @Mock
    private RaspberryPiService raspberryPiService;

    @Mock
    private RoomService roomService;

    @InjectMocks
    private SensorStationService service;

    private SensorStation station;

    @BeforeEach
    void setUp() {
        station = new SensorStation();
        station.setName("Station A");
        station.setBleMac("AA:BB:CC:DD:EE:FF");
        station.setDeviceStatus(DeviceStatus.AVAILABLE);
        station.setMeasurementInterval(30);
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("Should return all active sensor stations")
    void getAll_returnsAllActiveStations() {
        Mockito.when(repo.findAllActive()).thenReturn(List.of(station));

        List<SensorStation> result = service.getAll();

        Assertions.assertThat(result).containsExactly(station);
        Mockito.verify(repo).findAllActive();
    }

    @Test
    @WithMockUser(authorities = "BUILDING_ADMIN")
    @DisplayName("Should allow building admin to fetch all stations")
    void getAll_buildingAdminCanFetch() {
        Mockito.when(repo.findAllActive()).thenReturn(List.of());

        List<SensorStation> result = service.getAll();

        Assertions.assertThat(result).isEmpty();
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("Should return empty list when no stations exist")
    void getAll_returnsEmptyList() {
        Mockito.when(repo.findAllActive()).thenReturn(List.of());

        Assertions.assertThat(service.getAll()).isEmpty();
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("Should return sensor station when found by ID")
    void getById_returnsStation() {
        Mockito.when(repo.findById(1L)).thenReturn(Optional.of(station));

        SensorStation result = service.getById(1L);

        Assertions.assertThat(result).isEqualTo(station);
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("Should throw NotFoundException when station is not found")
    void getById_throwsNotFound() {
        Mockito.when(repo.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @WithMockUser(authorities = "BUILDING_ADMIN")
    @DisplayName("Should allow building admin to fetch station by ID")
    void getById_buildingAdmin() {
        Mockito.when(repo.findById(1L)).thenReturn(Optional.of(station));

        Assertions.assertThat(service.getById(1L)).isEqualTo(station);
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("Should save and return a new sensor station")
    void create_savesStation() {
        Mockito.when(repo.save(station)).thenReturn(station);

        SensorStation result = service.create(station);

        Assertions.assertThat(result).isEqualTo(station);
        Mockito.verify(repo).save(station);
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("Should persist station with RaspberryPi and Room relationships")
    void create_withRelations() {
        RaspberryPi pi = new RaspberryPi();
        Room room = new Room();

        station.setRaspberryPi(pi);
        station.setRoom(room);

        Mockito.when(repo.save(station)).thenReturn(station);

        SensorStation result = service.create(station);

        Assertions.assertThat(result.getRaspberryPi()).isEqualTo(pi);
        Assertions.assertThat(result.getRoom()).isEqualTo(room);
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("Should update room when a new room ID is provided in DTO")
    void update_roomId_updatesRoom() {
        Room room = new Room();

        SensorStationUpdateDTO dto = new SensorStationUpdateDTO(null, 2L, null, null);

        Mockito.when(repo.findById(1L)).thenReturn(Optional.of(station));
        Mockito.when(roomService.getById(2L)).thenReturn(room);
        Mockito.when(repo.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        SensorStation result = service.update(1L, dto);

        Assertions.assertThat(result.getRoom()).isEqualTo(room);
        Mockito.verify(roomService).getById(2L);
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("Should update device status when provided in DTO")
    void update_status() {
        SensorStationUpdateDTO dto = new SensorStationUpdateDTO(null, 1L, "x", DeviceStatus.DECOMMISSIONED);

        Mockito.when(repo.findById(1L)).thenReturn(Optional.of(station));
        Mockito.when(repo.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        SensorStation result = service.update(1L, dto);

        Assertions.assertThat(result.getDeviceStatus()).isEqualTo(DeviceStatus.DECOMMISSIONED);
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("Should link RaspberryPi when ID is provided")
    void update_raspberryPi() {
        RaspberryPi pi = new RaspberryPi();
        SensorStationUpdateDTO dto =
                new SensorStationUpdateDTO(10L, null, null, null);

        Mockito.when(repo.findById(1L)).thenReturn(Optional.of(station));
        Mockito.when(raspberryPiService.getById(10L)).thenReturn(pi);
        Mockito.when(repo.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        SensorStation result = service.update(1L, dto);

        Assertions.assertThat(result.getRaspberryPi()).isEqualTo(pi);
        Mockito.verify(raspberryPiService).getById(10L);
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("Should link room when ID is provided")
    void update_room() {
        Room room = new Room();
        SensorStationUpdateDTO dto =
                new SensorStationUpdateDTO(null, 20L, null, null);

        Mockito.when(repo.findById(1L)).thenReturn(Optional.of(station));
        Mockito.when(roomService.getById(20L)).thenReturn(room);
        Mockito.when(repo.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        SensorStation result = service.update(1L, dto);

        Assertions.assertThat(result.getRoom()).isEqualTo(room);
        Mockito.verify(roomService).getById(20L);
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("Should ignore null fields in update DTO")
    void update_nullFields() {
        SensorStationUpdateDTO dto = new SensorStationUpdateDTO(null, null, null, null);

        Mockito.when(repo.findById(1L)).thenReturn(Optional.of(station));
        Mockito.when(repo.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        SensorStation result = service.update(1L, dto);

        Assertions.assertThat(result.getName()).isEqualTo("Station A");
        Assertions.assertThat(result.getDeviceStatus()).isEqualTo(DeviceStatus.AVAILABLE);
        Mockito.verifyNoInteractions(raspberryPiService, roomService);
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("Should throw NotFoundException when updating non-existing station")
    void update_notFound() {
        SensorStationUpdateDTO dto = new SensorStationUpdateDTO(1L, null, null, null);

        Mockito.when(repo.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> service.update(99L, dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @WithMockUser(authorities = "BUILDING_ADMIN")
    @DisplayName("Should allow building admin to update station")
    void update_buildingAdmin() {
        SensorStationUpdateDTO dto = new SensorStationUpdateDTO(1L, null, null, null);

        Mockito.when(repo.findById(1L)).thenReturn(Optional.of(station));
        Mockito.when(repo.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        Assertions.assertThatCode(() -> service.update(1L, dto)).doesNotThrowAnyException();
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("Should delete sensor station by ID")
    void delete_success() {
        Mockito.doNothing().when(repo).deleteById(1L);

        service.delete(1L);

        Mockito.verify(repo).deleteById(1L);
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("Should propagate exception when repository delete fails")
    void delete_exception() {
        Mockito.doThrow(new RuntimeException("DB error")).when(repo).deleteById(1L);

        Assertions.assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB error");
    }
}