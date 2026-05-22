package at.qe.skeleton.tests.services;

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
import org.springframework.test.util.ReflectionTestUtils;

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
    @DisplayName("Should return all Raspberry Pi devices through internal path")
    void getAllInternal_returnsAllPis() {
        RaspberryPi inactivePi = new RaspberryPi();
        Mockito.when(repo.findAll()).thenReturn(List.of(pi, inactivePi));

        List<RaspberryPi> result = raspberryPiService.getAllInternal();

        Assertions.assertThat(result).containsExactly(pi, inactivePi);
        Mockito.verify(repo).findAll();
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
        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 5L);

        pi.setRoom(room);

        Mockito.when(roomService.getById(5L)).thenReturn(room);
        Mockito.when(repo.save(pi)).thenReturn(pi);

        RaspberryPi result = raspberryPiService.create(pi);

        Assertions.assertThat(result).isEqualTo(pi);
        Assertions.assertThat(result.getRoom()).isEqualTo(room);
        Assertions.assertThat(room.getRaspberryPi()).isEqualTo(pi);

        Mockito.verify(roomService).getById(5L);
        Mockito.verify(repo).save(pi);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when creating Raspberry Pi without assigned room")
    void create_piWithNullRoom_throwsIllegalArgumentException() {
        pi.setRoom(null);

        Assertions.assertThatThrownBy(() -> raspberryPiService.create(pi))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("room id");

        Mockito.verifyNoInteractions(roomService);
        Mockito.verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when creating Raspberry Pi with room without ID")
    void create_piWithRoomWithoutId_throwsIllegalArgumentException() {
        Room room = new Room();
        pi.setRoom(room);

        Assertions.assertThatThrownBy(() -> raspberryPiService.create(pi))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("room id");

        Mockito.verifyNoInteractions(roomService);
        Mockito.verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when creating Raspberry Pi in room that already has one")
    void create_roomAlreadyHasRaspberryPi_throwsIllegalArgumentException() {
        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 5L);

        RaspberryPi existingPi = new RaspberryPi();
        room.setRaspberryPi(existingPi);

        pi.setRoom(room);

        Mockito.when(roomService.getById(5L)).thenReturn(room);

        Assertions.assertThatThrownBy(() -> raspberryPiService.create(pi))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Room already has a RaspberryPi");

        Mockito.verify(roomService).getById(5L);
        Mockito.verifyNoInteractions(repo);
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
    @DisplayName("Should update Raspberry Pi through internal update path")
    void updateInternal_updatesRaspberryPi() {
        RaspberryPiUpdateDTO dto = new RaspberryPiUpdateDTO(
                "10.0.0.2",
                "raspi-office",
                DeviceStatus.ONLINE,
                null,
                null
        );
        Mockito.when(repo.findById(1L)).thenReturn(Optional.of(pi));
        Mockito.when(repo.save(pi)).thenReturn(pi);

        raspberryPiService.updateInternal(1L, dto);

        Assertions.assertThat(pi.getIpAddress()).isEqualTo("10.0.0.2");
        Assertions.assertThat(pi.getHostName()).isEqualTo("raspi-office");
        Assertions.assertThat(pi.getDeviceStatus()).isEqualTo(DeviceStatus.ONLINE);
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
    @DisplayName("Should return only available sensor stations for a Raspberry Pi")
    void getAvailableSensorStations_returnsOnlyAvailableStations_forExistingPi() {
        SensorStation available = new SensorStation();
        available.setDeviceStatus(DeviceStatus.AVAILABLE);

        SensorStation online = new SensorStation();
        online.setDeviceStatus(DeviceStatus.ONLINE);

        SensorStation noStatus = new SensorStation();

        pi.getSensorStations().add(available);
        pi.getSensorStations().add(online);
        pi.getSensorStations().add(noStatus);
        Mockito.when(repo.findById(1L)).thenReturn(Optional.of(pi));

        List<SensorStation> result = raspberryPiService.getAvailableSensorStations(1L);

        Assertions.assertThat(result).containsExactly(available);
    }

    @Test
    @DisplayName("Should throw NotFoundException when Raspberry Pi does not exist for available sensor stations")
    void getAvailableSensorStations_throwsNotFoundException_whenPiNotFound() {
        Mockito.when(repo.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> raspberryPiService.getAvailableSensorStations(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Should delete Raspberry Pi and clear associations")
    void delete_deletesRaspberryPiAndClearsAssociations() {
        Room room = new Room();
        room.setRaspberryPi(pi);
        pi.setRoom(room);

        SensorStation station = new SensorStation();
        station.setRaspberryPi(pi);
        pi.getSensorStations().add(station);

        Mockito.when(repo.findById(1L)).thenReturn(Optional.of(pi));

        raspberryPiService.delete(1L);

        Assertions.assertThat(pi.getRoom()).isNull();
        Assertions.assertThat(room.getRaspberryPi()).isNull();

        Assertions.assertThat(pi.getSensorStations()).isEmpty();
        Assertions.assertThat(station.getRaspberryPi()).isNull();

        Mockito.verify(repo).findById(1L);
        Mockito.verify(repo).delete(pi);
        Mockito.verify(repo, Mockito.never()).deleteById(Mockito.anyLong());
    }

    @Test
    @DisplayName("Should throw NotFoundException when deleting non-existing Raspberry Pi")
    void delete_throwsNotFoundException_whenPiDoesNotExist() {
        Mockito.when(repo.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> raspberryPiService.delete(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");

        Mockito.verify(repo).findById(99L);
        Mockito.verify(repo, Mockito.never()).delete(Mockito.any());
        Mockito.verify(repo, Mockito.never()).deleteById(Mockito.anyLong());
    }

    @Test
    @DisplayName("Should add newly discovered sensor stations as available")
    void addAvailableSensorStations_createsAvailableSensorStations() {
        Room room = new Room();
        pi.setRoom(room);

        Mockito.when(repo.findById(1L)).thenReturn(Optional.of(pi));
        Mockito.when(sensorStationRepository.findByBleMac("AA:BB:CC:DD:EE:FF")).thenReturn(Optional.empty());
        Mockito.when(sensorStationRepository.save(Mockito.any(SensorStation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        raspberryPiService.addAvailableSensorStations(1L, List.of("AA:BB:CC:DD:EE:FF"));

        Mockito.verify(sensorStationRepository).save(Mockito.argThat(station ->
                "AA:BB:CC:DD:EE:FF".equals(station.getBleMac())
                        && "AA:BB:CC:DD:EE:FF".equals(station.getName())
                        && station.getRaspberryPi() == pi
                        && station.getRoom() == room
                        && station.getDeviceStatus() == DeviceStatus.AVAILABLE
                        && Integer.valueOf(60).equals(station.getMeasurementInterval())
        ));
    }

    @Test
    @DisplayName("Should update existing discovered sensor station and keep configured measurement interval")
    void addAvailableSensorStations_updatesExistingStation() {
        Room room = new Room();
        pi.setRoom(room);

        SensorStation station = new SensorStation();
        ReflectionTestUtils.setField(station, "id", 11L);
        station.setMeasurementInterval(30);

        Mockito.when(repo.findById(1L)).thenReturn(Optional.of(pi));
        Mockito.when(sensorStationRepository.findByBleMac("AA:BB:CC:DD:EE:FF")).thenReturn(Optional.of(station));
        Mockito.when(sensorStationRepository.save(station)).thenReturn(station);

        raspberryPiService.addAvailableSensorStations(1L, List.of("AA:BB:CC:DD:EE:FF"));

        Assertions.assertThat(station.getBleMac()).isEqualTo("AA:BB:CC:DD:EE:FF");
        Assertions.assertThat(station.getName()).isEqualTo("AA:BB:CC:DD:EE:FF");
        Assertions.assertThat(station.getRaspberryPi()).isEqualTo(pi);
        Assertions.assertThat(station.getRoom()).isEqualTo(room);
        Assertions.assertThat(station.getDeviceStatus()).isEqualTo(DeviceStatus.AVAILABLE);
        Assertions.assertThat(station.getMeasurementInterval()).isEqualTo(30);
        Mockito.verify(sensorStationRepository).save(station);
    }

    @Test
    @DisplayName("Should delete available sensor station after scan timeout")
    void removeAvailableSensorStationAfterScanTimeOut_removesAndDeletesStation() {
        SensorStation station = new SensorStation();
        ReflectionTestUtils.setField(station, "id", 11L);
        station.setRaspberryPi(pi);
        pi.getSensorStations().add(station);

        Mockito.when(repo.findById(1L)).thenReturn(Optional.of(pi));
        Mockito.when(sensorStationRepository.findById(11L)).thenReturn(Optional.of(station));

        raspberryPiService.removeAvailableSensorStationAfterScanTimeOut(1L, 11L);

        Assertions.assertThat(pi.getSensorStations()).doesNotContain(station);
        Mockito.verify(sensorStationRepository).delete(station);
    }

}
