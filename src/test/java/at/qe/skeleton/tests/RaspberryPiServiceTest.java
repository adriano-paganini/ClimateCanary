package at.qe.skeleton.tests;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.RaspberryPiUpdateDTO;
import at.qe.skeleton.models.DeviceStatus;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.models.SensorStation;
import at.qe.skeleton.repositories.RaspberryPiRepository;
import at.qe.skeleton.repositories.RoomRepository;
import at.qe.skeleton.repositories.SensorStationRepository;
import at.qe.skeleton.services.RaspberryPiService;
import at.qe.skeleton.services.RoomService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class RaspberryPiServiceTest {

    @Mock
    private RaspberryPiRepository repo;

    @Mock
    private RoomService roomService;

    @Mock
    private SensorStationRepository sensorStationRepository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RaspberryPiService raspberryPiService;

    private RaspberryPi pi;

    @BeforeEach
    void setUp() {
        pi = new RaspberryPi();
        pi.setIpAddress("192.168.1.100");
        pi.setSensorStations(new ArrayList<>());
    }

    @Test
    @DisplayName("Should return all active Raspberry Pi devices")
    void getAll_returnsAllActivePis() {
        Mockito.when(repo.findAllActive()).thenReturn(List.of(pi));

        List<RaspberryPi> result = raspberryPiService.getAll();

        Assertions.assertThat(result).containsExactly(pi);
        Mockito.verify(repo).findAllActive();
    }

    @Test
    @DisplayName("Should return empty list when no active Raspberry Pi devices exist")
    void getAll_returnsEmptyListwhenNoneExist() {
        Mockito.when(repo.findAllActive()).thenReturn(List.of());

        Assertions.assertThat(raspberryPiService.getAll()).isEmpty();
    }

    @Test
    @DisplayName("Should return Raspberry Pi when ID exists")
    void getById_returnsRaspberryPi_whenFound() {
        Mockito.when(repo.findById(1L)).thenReturn(Optional.of(pi));

        RaspberryPi result = raspberryPiService.getById(1L);

        Assertions.assertThat(result).isEqualTo(pi);
    }

    @Test
    @DisplayName("Should throw NotFoundException when Raspberry Pi ID does not exist")
    void getById_throwsNotFoundException_whenNotFound() {
        Mockito.when(repo.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> raspberryPiService.getById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Should save and return new Raspberry Pi")
    void create_savesAndReturnsRaspberryPi() {
        Mockito.when(repo.save(pi)).thenReturn(pi);

        RaspberryPi result = raspberryPiService.create(pi);

        Assertions.assertThat(result).isEqualTo(pi);
        Mockito.verify(repo).save(pi);
    }

    @Test
    @DisplayName("Should create Raspberry Pi successfully even without assigned room")
    void create_piWithNullRoom_doesNotThrow() {
        pi.setRoom(null);
        Mockito.when(repo.save(pi)).thenReturn(pi);

        Assertions.assertThatCode(() -> raspberryPiService.create(pi)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should update IP address of Raspberry Pi")
    void update_updatesIpAddress() {
        RaspberryPiUpdateDTO dto = new RaspberryPiUpdateDTO("10.0.0.1", null, null, null, null);
        Mockito.when(repo.findById(1L)).thenReturn(Optional.of(pi));
        Mockito.when(repo.save(pi)).thenReturn(pi);

        RaspberryPi result = raspberryPiService.update(1L, dto);

        Assertions.assertThat(result.getIpAddress()).isEqualTo("10.0.0.1");
        Mockito.verify(repo).save(pi);
    }

    @Test
    @DisplayName("Should update assigned room when room ID is provided")
    void update_updatesRoom_whenRoomIdProvided() {
        Room room = new Room();
        roomRepository.save(room);
        RaspberryPiUpdateDTO dto = new RaspberryPiUpdateDTO(null, null, DeviceStatus.AVAILABLE, 5L, null);

        Mockito.when(repo.findById(1L)).thenReturn(Optional.of(pi));
        Mockito.when(roomService.getById(5L)).thenReturn(room);
        Mockito.when(repo.save(pi)).thenReturn(pi);

        RaspberryPi result = raspberryPiService.update(1L, dto);

        Assertions.assertThat(result.getRoom()).isEqualTo(room);
    }

    @Test
    @DisplayName("Should replace sensor stations and clear previous associations")
    void update_updatesSensorStations_andClearsPreviousAssociation() {
        SensorStation oldStation = new SensorStation();
        oldStation.setRaspberryPi(pi);
        pi.getSensorStations().add(oldStation);

        SensorStation newStation = new SensorStation();
        RaspberryPiUpdateDTO dto =
                new RaspberryPiUpdateDTO(null, null, null, null, List.of(Long.valueOf(20L)));

        Mockito.when(repo.findById(1L)).thenReturn(Optional.of(pi));
        Mockito.when(sensorStationRepository.findAllById(List.of(20L))).thenReturn(List.of(newStation));
        Mockito.when(repo.save(pi)).thenReturn(pi);

        RaspberryPi result = raspberryPiService.update(1L, dto);

        Assertions.assertThat(result.getSensorStations()).containsExactly(newStation);
        Assertions.assertThat(oldStation.getRaspberryPi()).isNull();
        Assertions.assertThat(newStation.getRaspberryPi()).isEqualTo(pi);
    }

    @Test
    @DisplayName("Should not modify Raspberry Pi when update DTO fields are null")
    void update_doesNotModifyFields_MockitoDTOFieldsAreNull() {
        RaspberryPiUpdateDTO dto = new RaspberryPiUpdateDTO(null, null, null, null, null);
        Mockito.when(repo.findById(1L)).thenReturn(Optional.of(pi));
        Mockito.when(repo.save(pi)).thenReturn(pi);

        RaspberryPi result = raspberryPiService.update(1L, dto);

        Assertions.assertThat(result.getIpAddress()).isEqualTo("192.168.1.100");
        Mockito.verifyNoInteractions(roomService, sensorStationRepository);
    }

    @Test
    @DisplayName("Should throw NotFoundException when updating non-existing Raspberry Pi")
    void update_throwsNotFoundException_whenPiNotFound() {
        RaspberryPiUpdateDTO dto = new RaspberryPiUpdateDTO(null, null, null, null, null);
        Mockito.when(repo.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> raspberryPiService.update(99L, dto))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Should return all sensor stations for a Raspberry Pi")
    void getSensorStations_returnsSensorStations_forExistingPi() {
        SensorStation s = new SensorStation();
        pi.getSensorStations().add(s);
        Mockito.when(repo.findById(1L)).thenReturn(Optional.of(pi));

        List<SensorStation> result = raspberryPiService.getSensorStations(1L);

        Assertions.assertThat(result).containsExactly(s);
    }

    @Test
    @DisplayName("Should throw NotFoundException when Raspberry Pi does not exist")
    void getSensorStations_throwsNotFoundException_whenPiNotFound() {
        Mockito.when(repo.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> raspberryPiService.getSensorStations(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Should delete Raspberry Pi by ID")
    void delete_callsDeleteById() {
        raspberryPiService.delete(1L);

        Mockito.verify(repo).deleteById(1L);
    }

    @Test
    @DisplayName("Should not throw exception when deleting non-existing Raspberry Pi")
    void delete_doesNotThrow_whenPiDoesNotExist() {
        Mockito.doNothing().when(repo).deleteById(99L);

        Assertions.assertThatCode(() -> raspberryPiService.delete(99L)).doesNotThrowAnyException();
    }
}