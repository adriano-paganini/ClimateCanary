package at.qe.skeleton.tests.scheduled;

import at.qe.skeleton.dtos.RaspberryPiUpdateDTO;
import at.qe.skeleton.models.DeviceStatus;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.scheduled.ScheduledMethods;
import at.qe.skeleton.services.RaspberryPiServerService;
import at.qe.skeleton.services.RaspberryPiService;
import at.qe.skeleton.services.RoomPrivacyModeService;
import at.qe.skeleton.services.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class ScheduledMethodsTest {

    @Mock
    private RoomService roomService;

    @Mock
    private RaspberryPiServerService raspberryPiServerService;

    @Mock
    private RaspberryPiService raspberryPiService;

    @Mock
    private RoomPrivacyModeService roomPrivacyModeService;

    private ScheduledMethods scheduledMethods;

    @BeforeEach
    void setUp() {
        scheduledMethods = new ScheduledMethods(
                roomService,
                raspberryPiServerService,
                raspberryPiService,
                roomPrivacyModeService
        );
    }

    @Test
    @DisplayName("updates privacy mode for all rooms")
    void updatePrivacyModeForAllRaspberryPis_updatesEveryRoom() {
        Room room1 = new Room();
        ReflectionTestUtils.setField(room1, "id", 1L);

        Room room2 = new Room();
        ReflectionTestUtils.setField(room2, "id", 2L);

        Mockito.when(roomService.getAll()).thenReturn(List.of(room1, room2));

        scheduledMethods.updatePrivacyModeForAllRaspberryPis();

        Mockito.verify(roomPrivacyModeService).updatePrivacyModeForRoom(1L);
        Mockito.verify(roomPrivacyModeService).updatePrivacyModeForRoom(2L);
        Mockito.verifyNoMoreInteractions(roomPrivacyModeService);
    }

    @Test
    @DisplayName("updates privacy mode even for rooms without Raspberry Pi")
    void updatePrivacyModeForAllRaspberryPis_updatesRoomsWithoutRaspberryPi() {
        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 1L);
        room.setRaspberryPi(null);

        Mockito.when(roomService.getAll()).thenReturn(List.of(room));

        scheduledMethods.updatePrivacyModeForAllRaspberryPis();

        Mockito.verify(roomPrivacyModeService).updatePrivacyModeForRoom(1L);
    }

    @Test
    @DisplayName("does nothing when there are no rooms")
    void updatePrivacyModeForAllRaspberryPis_noRooms_doesNothing() {
        Mockito.when(roomService.getAll()).thenReturn(List.of());

        scheduledMethods.updatePrivacyModeForAllRaspberryPis();

        Mockito.verifyNoInteractions(roomPrivacyModeService);
    }

    @Test
    @DisplayName("sets Raspberry Pi online when heartbeat succeeds")
    void checkHeartbeat_online_updatesStatus() {
        RaspberryPi pi = new RaspberryPi();
        ReflectionTestUtils.setField(pi, "id", 1L);
        pi.setDeviceStatus(DeviceStatus.OFFLINE);

        Mockito.when(raspberryPiService.getAllInternal()).thenReturn(List.of(pi));
        Mockito.when(raspberryPiServerService.getHeartbeat(1L)).thenReturn(true);

        scheduledMethods.checkHeartbeat();

        Mockito.verify(raspberryPiService).updateInternal(
                Mockito.eq(1L),
                Mockito.any(RaspberryPiUpdateDTO.class)
        );
    }

    @Test
    @DisplayName("sets Raspberry Pi offline when heartbeat fails")
    void checkHeartbeat_offline_updatesStatus() {
        RaspberryPi pi = new RaspberryPi();
        ReflectionTestUtils.setField(pi, "id", 1L);
        pi.setDeviceStatus(DeviceStatus.ONLINE);

        Mockito.when(raspberryPiService.getAllInternal()).thenReturn(List.of(pi));
        Mockito.when(raspberryPiServerService.getHeartbeat(1L)).thenReturn(false);

        scheduledMethods.checkHeartbeat();

        Mockito.verify(raspberryPiService).updateInternal(
                Mockito.eq(1L),
                Mockito.any(RaspberryPiUpdateDTO.class)
        );
    }

    @Test
    @DisplayName("does not update Raspberry Pi status when heartbeat status is unchanged online")
    void checkHeartbeat_onlineUnchanged_doesNotUpdate() {
        RaspberryPi pi = new RaspberryPi();
        ReflectionTestUtils.setField(pi, "id", 1L);
        pi.setDeviceStatus(DeviceStatus.ONLINE);

        Mockito.when(raspberryPiService.getAllInternal()).thenReturn(List.of(pi));
        Mockito.when(raspberryPiServerService.getHeartbeat(1L)).thenReturn(true);

        scheduledMethods.checkHeartbeat();

        Mockito.verify(raspberryPiService, Mockito.never()).updateInternal(
                Mockito.anyLong(),
                Mockito.any(RaspberryPiUpdateDTO.class)
        );
    }

    @Test
    @DisplayName("does not update Raspberry Pi status when heartbeat status is unchanged offline")
    void checkHeartbeat_offlineUnchanged_doesNotUpdate() {
        RaspberryPi pi = new RaspberryPi();
        ReflectionTestUtils.setField(pi, "id", 1L);
        pi.setDeviceStatus(DeviceStatus.OFFLINE);

        Mockito.when(raspberryPiService.getAllInternal()).thenReturn(List.of(pi));
        Mockito.when(raspberryPiServerService.getHeartbeat(1L)).thenReturn(false);

        scheduledMethods.checkHeartbeat();

        Mockito.verify(raspberryPiService, Mockito.never()).updateInternal(
                Mockito.anyLong(),
                Mockito.any(RaspberryPiUpdateDTO.class)
        );
    }

    @Test
    @DisplayName("checks heartbeat for all Raspberry Pis")
    void checkHeartbeat_multiplePis_checksAll() {
        RaspberryPi pi1 = new RaspberryPi();
        ReflectionTestUtils.setField(pi1, "id", 1L);
        pi1.setDeviceStatus(DeviceStatus.ONLINE);

        RaspberryPi pi2 = new RaspberryPi();
        ReflectionTestUtils.setField(pi2, "id", 2L);
        pi2.setDeviceStatus(DeviceStatus.OFFLINE);

        Mockito.when(raspberryPiService.getAllInternal()).thenReturn(List.of(pi1, pi2));
        Mockito.when(raspberryPiServerService.getHeartbeat(1L)).thenReturn(true);
        Mockito.when(raspberryPiServerService.getHeartbeat(2L)).thenReturn(false);

        scheduledMethods.checkHeartbeat();

        Mockito.verify(raspberryPiServerService).getHeartbeat(1L);
        Mockito.verify(raspberryPiServerService).getHeartbeat(2L);
        Mockito.verify(raspberryPiService, Mockito.never()).updateInternal(
                Mockito.anyLong(),
                Mockito.any(RaspberryPiUpdateDTO.class)
        );
    }
}