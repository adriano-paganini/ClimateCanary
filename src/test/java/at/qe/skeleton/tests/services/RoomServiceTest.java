package at.qe.skeleton.tests.services;

import at.qe.skeleton.common.exceptions.ConflictException;
import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.RoomUpdateDTO;
import at.qe.skeleton.models.*;
import at.qe.skeleton.repositories.RoomRepository;
import at.qe.skeleton.repositories.EmployeeProfileRepository;
import at.qe.skeleton.services.BuildingService;
import at.qe.skeleton.services.DepartmentService;
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
import org.springframework.security.test.context.support.WithMockUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private BuildingService buildingService;

    @Mock
    private EmployeeProfileRepository employeeProfileRepository;

    @InjectMocks
    private RoomService roomService;

    private Room room;
    private Department department;
    private Building building;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setRooms(new ArrayList<>());

        building = new Building();
        building.setRooms(new ArrayList<>());

        room = new Room();
        room.setName("Room A");
        room.setRoomType(RoomType.OFFICE);
        room.setPrivacyMode(false);
        room.setActive(true);
        room.setDepartment(department);
        room.setBuilding(building);
    }

    @Test
    @DisplayName("getAll returns only active rooms")
    void getAll_returnsOnlyActiveRooms() {
        List<Room> activeRooms = List.of(room);
        Mockito.when(roomRepository.findAllByActiveTrue()).thenReturn(activeRooms);

        List<Room> result = roomService.getAll();

        Assertions.assertThat(result).containsExactly(room);
        Mockito.verify(roomRepository).findAllByActiveTrue();
    }

    @Test
    @DisplayName("getAll returns empty list when no active rooms exist")
    void getAll_returnsEmptyList_NoActiveRooms() {
        Mockito.when(roomRepository.findAllByActiveTrue()).thenReturn(List.of());

        Assertions.assertThat(roomService.getAll()).isEmpty();
    }

    @Test
    @DisplayName("getById returns room when it exists and is active")
    void getById_returnsRoom_whenExists() {
        Mockito.when(roomRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(room));

        Room result = roomService.getById(1L);

        Assertions.assertThat(result).isEqualTo(room);
    }

    @Test
    @DisplayName("getById throws NotFoundException when room is not found")
    void getById_throwsNotFoundException_NotFound() {
        Mockito.when(roomRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> roomService.getById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("getById throws NotFoundException when room is inactive")
    void getById_throwsNotFoundException_RoomIsInactive() {
        Mockito.when(roomRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> roomService.getById(1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("create saves and returns room")
    void create_savesAndReturnsRoom() {
        Mockito.when(roomRepository.save(room)).thenReturn(room);

        Room result = roomService.create(room);

        Assertions.assertThat(result).isEqualTo(room);
        Mockito.verify(roomRepository).save(room);
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("create persists room with null department and building")
    void create_persistsRoomWithNullDepartmentAndBuilding() {
        Room minimal = new Room();
        minimal.setName("Minimal");
        minimal.setDepartment(null);
        minimal.setBuilding(null);

        Mockito.when(roomRepository.save(minimal)).thenReturn(minimal);

        Room result = roomService.create(minimal);

        Assertions.assertThat(result).isEqualTo(minimal);
        Mockito.verify(roomRepository).save(minimal);
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("update applies all fields from DTO")
    void update_appliesAllFieldsFromDTO() {
        Department newDept = new Department();
        Building newBuilding = new Building();

        RoomUpdateDTO dto = new RoomUpdateDTO("Room B", RoomType.COMMON_AREAS, true, 11L, 21L);

        Mockito.when(roomRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(room));
        Mockito.when(departmentService.getDepartmentById(11L)).thenReturn(newDept);
        Mockito.when(buildingService.getBuildingById(21L)).thenReturn(newBuilding);
        Mockito.when(roomRepository.save(Mockito.any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        Room result = roomService.update(1L, dto);

        Assertions.assertThat(result.getName()).isEqualTo("Room B");
        Assertions.assertThat(result.getRoomType()).isEqualTo(RoomType.COMMON_AREAS);
        Assertions.assertThat(result.getPrivacyMode()).isTrue();
        Assertions.assertThat(result.getDepartment()).isEqualTo(newDept);
        Assertions.assertThat(result.getBuilding()).isEqualTo(newBuilding);
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("update ignores null fields and keeps existing values")
    void update_ignoresNullFields_leavingExistingValuesIntact() {
        RoomUpdateDTO dto = new RoomUpdateDTO(null, null, null, null, null);

        Mockito.when(roomRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(room));
        Mockito.when(roomRepository.save(Mockito.any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        Room result = roomService.update(1L, dto);

        Assertions.assertThat(result.getName()).isEqualTo("Room A");
        Assertions.assertThat(result.getRoomType()).isEqualTo(RoomType.OFFICE);
        Mockito.verifyNoInteractions(departmentService, buildingService);
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("update throws NotFoundException when room does not exist")
    void update_throwsNotFoundException_whenRoomDoesNotExist() {
        RoomUpdateDTO dto = new RoomUpdateDTO("X", null, null, null, null);
        Mockito.when(roomRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> roomService.update(99L, dto))
                .isInstanceOf(NotFoundException.class);

        Mockito.verify(roomRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("delete marks room inactive and clears associations")
    void delete_marksRoomInactiveAndClearsAssociations() {
        department.getRooms().add(room);
        building.getRooms().add(room);

        Mockito.when(roomRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(room));

        roomService.delete(1L);

        Assertions.assertThat(room.isActive()).isFalse();
        Assertions.assertThat(room.getDepartment()).isNull();
        Assertions.assertThat(room.getBuilding()).isNull();
        Assertions.assertThat(department.getRooms()).doesNotContain(room);
        Assertions.assertThat(building.getRooms()).doesNotContain(room);
        Mockito.verify(roomRepository, Mockito.never()).deleteById(Mockito.any());
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("delete decommissions all sensor stations in the room")
    void delete_decommissionsSensorStations() {
        SensorStation ss = new SensorStation();
        ss.setDeviceStatus(DeviceStatus.AVAILABLE);
        room.getSensorStations().add(ss);

        Mockito.when(roomRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(room));

        roomService.delete(1L);

        Assertions.assertThat(ss.getDeviceStatus()).isEqualTo(DeviceStatus.DECOMMISSIONED);
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("delete decommissions raspberry pi when present")
    void delete_decommissionsRaspberryPi_whenPresent() {
        RaspberryPi pi = new RaspberryPi();
        pi.setDeviceStatus(DeviceStatus.AVAILABLE);
        room.setRaspberryPi(pi);

        Mockito.when(roomRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(room));

        roomService.delete(1L);

        Assertions.assertThat(pi.getDeviceStatus()).isEqualTo(DeviceStatus.DECOMMISSIONED);
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("delete throws ConflictException when employee profiles still reference room")
    void delete_throwsConflictException_whenEmployeeProfilesReferenceRoom() {
        EmployeeProfile ep1 = new EmployeeProfile();
        ep1.setRoom(room);
        EmployeeProfile ep2 = new EmployeeProfile();
        ep2.setRoom(room);
        room.getEmployeeProfiles().add(ep1);
        room.getEmployeeProfiles().add(ep2);

        Mockito.when(roomRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(room));
        Mockito.when(employeeProfileRepository.countByRoom_Id(1L)).thenReturn(2L);

        Assertions.assertThatThrownBy(() -> roomService.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("2 assigned employees");

        Assertions.assertThat(room.isActive()).isTrue();
        Assertions.assertThat(ep1.getRoom()).isEqualTo(room);
        Assertions.assertThat(ep2.getRoom()).isEqualTo(room);
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("delete handles null department and building gracefully")
    void delete_handlesNullDepartmentAndBuilding_gracefully() {
        room.setDepartment(null);
        room.setBuilding(null);

        Mockito.when(roomRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(room));

        Assertions.assertThatCode(() -> roomService.delete(1L))
                .doesNotThrowAnyException();
        Assertions.assertThat(room.isActive()).isFalse();
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("delete handles null raspberry pi gracefully")
    void delete_handlesNullRaspberryPi_gracefully() {
        room.setRaspberryPi(null);

        Mockito.when(roomRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(room));

        Assertions.assertThatCode(() -> roomService.delete(1L))
                .doesNotThrowAnyException();
    }

    @Test
    @WithMockUser(authorities = "SYSTEM_ADMIN")
    @DisplayName("delete throws NotFoundException when room does not exist")
    void delete_throwsNotFoundException_RoomDoesNotExist() {
        Mockito.when(roomRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> roomService.delete(99L))
                .isInstanceOf(NotFoundException.class);
    }
}
