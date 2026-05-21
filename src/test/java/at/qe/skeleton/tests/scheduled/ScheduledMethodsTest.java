package at.qe.skeleton.tests.scheduled;

import at.qe.skeleton.dtos.RaspberryPiUpdateDTO;
import at.qe.skeleton.dtos.RoomUpdateDTO;
import at.qe.skeleton.models.Absence;
import at.qe.skeleton.models.DeviceStatus;
import at.qe.skeleton.models.EmployeeProfile;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.models.RoomType;
import at.qe.skeleton.models.Userx;
import at.qe.skeleton.scheduled.ScheduledMethods;
import at.qe.skeleton.services.AbsenceService;
import at.qe.skeleton.services.EmployeeProfileService;
import at.qe.skeleton.services.PiRequestResult;
import at.qe.skeleton.services.RaspberryPiServerService;
import at.qe.skeleton.services.RaspberryPiService;
import at.qe.skeleton.services.RoomService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ScheduledMethodsTest {

    @Mock
    private AbsenceService absenceService;

    @Mock
    private RoomService roomService;

    @Mock
    private EmployeeProfileService employeeProfileService;

    @Mock
    private RaspberryPiServerService raspberryPiServerService;

    @Mock
    private RaspberryPiService raspberryPiService;

    @InjectMocks
    private ScheduledMethods scheduledMethods;

    @Test
    void updatePrivacyModeForAllRaspberryPis_enablesPrivacyModeWhenOccupancyDropsBelowFive() {
        Room room = room(10L, RoomType.COMMON_AREAS, false);
        RaspberryPi raspberryPi = raspberryPi(5L, DeviceStatus.ONLINE);
        room.setRaspberryPi(raspberryPi);

        Mockito.when(absenceService.getByTimeframe(Mockito.any(LocalDateTime.class), Mockito.any(LocalDateTime.class)))
                .thenReturn(List.of(absence(1L)));
        Mockito.when(roomService.getAll()).thenReturn(List.of(room));
        Mockito.when(employeeProfileService.getAll(10L, null))
                .thenReturn(List.of(
                        employeeProfile(1L),
                        employeeProfile(2L),
                        employeeProfile(3L),
                        employeeProfile(4L),
                        employeeProfile(5L)
                ));
        Mockito.when(raspberryPiServerService.setOccupancy(5L, 10L, true))
                .thenReturn(PiRequestResult.SUCCESS);

        scheduledMethods.updatePrivacyModeForAllRaspberryPis();

        ArgumentCaptor<RoomUpdateDTO> captor = ArgumentCaptor.forClass(RoomUpdateDTO.class);
        Mockito.verify(raspberryPiServerService).setOccupancy(5L, 10L, true);
        Mockito.verify(roomService).update(Mockito.eq(10L), captor.capture());
        assertThat(captor.getValue().privacyMode()).isTrue();
    }

    @Test
    void updatePrivacyModeForAllRaspberryPis_doesNotPersistWhenPiUpdateFails() {
        Room room = room(10L, RoomType.COMMON_AREAS, false);
        RaspberryPi raspberryPi = raspberryPi(5L, DeviceStatus.ONLINE);
        room.setRaspberryPi(raspberryPi);

        Mockito.when(absenceService.getByTimeframe(Mockito.any(LocalDateTime.class), Mockito.any(LocalDateTime.class)))
                .thenReturn(List.of(absence(1L)));
        Mockito.when(roomService.getAll()).thenReturn(List.of(room));
        Mockito.when(employeeProfileService.getAll(10L, null))
                .thenReturn(List.of(
                        employeeProfile(1L),
                        employeeProfile(2L),
                        employeeProfile(3L),
                        employeeProfile(4L),
                        employeeProfile(5L)
                ));
        Mockito.when(raspberryPiServerService.setOccupancy(5L, 10L, true))
                .thenReturn(PiRequestResult.SERVER_ERROR);

        scheduledMethods.updatePrivacyModeForAllRaspberryPis();

        Mockito.verify(raspberryPiServerService).setOccupancy(5L, 10L, true);
        Mockito.verify(roomService, Mockito.never()).update(Mockito.anyLong(), Mockito.any(RoomUpdateDTO.class));
    }

    @Test
    void updatePrivacyModeForAllRaspberryPis_ignoresRoomsWithoutCommonAreaPi() {
        Room office = room(10L, RoomType.OFFICE, false);
        office.setRaspberryPi(raspberryPi(5L, DeviceStatus.ONLINE));

        Room commonAreaWithoutPi = room(11L, RoomType.COMMON_AREAS, false);

        Mockito.when(absenceService.getByTimeframe(Mockito.any(LocalDateTime.class), Mockito.any(LocalDateTime.class)))
                .thenReturn(List.of());
        Mockito.when(roomService.getAll()).thenReturn(List.of(office, commonAreaWithoutPi));

        scheduledMethods.updatePrivacyModeForAllRaspberryPis();

        Mockito.verifyNoInteractions(employeeProfileService, raspberryPiServerService);
        Mockito.verify(roomService, Mockito.never()).update(Mockito.anyLong(), Mockito.any(RoomUpdateDTO.class));
    }

    @Test
    void checkHeartbeat_updatesStatusWhenHeartbeatStateChanges() {
        RaspberryPi offlinePi = raspberryPi(5L, DeviceStatus.OFFLINE);
        RaspberryPi onlinePi = raspberryPi(6L, DeviceStatus.ONLINE);

        Mockito.when(raspberryPiService.getAllInternal()).thenReturn(List.of(offlinePi, onlinePi));
        Mockito.when(raspberryPiServerService.getHeartbeat(5L)).thenReturn(true);
        Mockito.when(raspberryPiServerService.getHeartbeat(6L)).thenReturn(false);

        scheduledMethods.checkHeartbeat();

        ArgumentCaptor<RaspberryPiUpdateDTO> captor = ArgumentCaptor.forClass(RaspberryPiUpdateDTO.class);
        Mockito.verify(raspberryPiService).updateInternal(Mockito.eq(5L), captor.capture());
        assertThat(captor.getValue().deviceStatus()).isEqualTo(DeviceStatus.ONLINE);

        Mockito.verify(raspberryPiService).updateInternal(Mockito.eq(6L), captor.capture());
        assertThat(captor.getAllValues().get(1).deviceStatus()).isEqualTo(DeviceStatus.OFFLINE);
    }

    @Test
    void checkHeartbeat_doesNotUpdateWhenStatusIsUnchanged() {
        RaspberryPi onlinePi = raspberryPi(5L, DeviceStatus.ONLINE);
        RaspberryPi offlinePi = raspberryPi(6L, DeviceStatus.OFFLINE);

        Mockito.when(raspberryPiService.getAllInternal()).thenReturn(List.of(onlinePi, offlinePi));
        Mockito.when(raspberryPiServerService.getHeartbeat(5L)).thenReturn(true);
        Mockito.when(raspberryPiServerService.getHeartbeat(6L)).thenReturn(false);

        scheduledMethods.checkHeartbeat();

        Mockito.verify(raspberryPiService, Mockito.never())
                .updateInternal(Mockito.anyLong(), Mockito.any(RaspberryPiUpdateDTO.class));
    }

    private Room room(Long id, RoomType roomType, Boolean privacyMode) {
        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", id);
        room.setRoomType(roomType);
        room.setPrivacyMode(privacyMode);
        return room;
    }

    private RaspberryPi raspberryPi(Long id, DeviceStatus deviceStatus) {
        RaspberryPi raspberryPi = new RaspberryPi();
        ReflectionTestUtils.setField(raspberryPi, "id", id);
        raspberryPi.setDeviceStatus(deviceStatus);
        return raspberryPi;
    }

    private EmployeeProfile employeeProfile(Long userId) {
        EmployeeProfile employeeProfile = new EmployeeProfile();
        employeeProfile.setUser(user(userId));
        return employeeProfile;
    }

    private Absence absence(Long userId) {
        Absence absence = new Absence();
        absence.setUser(user(userId));
        return absence;
    }

    private Userx user(Long id) {
        Userx user = new Userx();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
