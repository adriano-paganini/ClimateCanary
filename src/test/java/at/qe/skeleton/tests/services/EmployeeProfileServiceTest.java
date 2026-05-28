package at.qe.skeleton.tests.services;

import at.qe.skeleton.common.exceptions.ConflictException;
import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.EmployeeProfileUpdateDTO;
import at.qe.skeleton.models.Department;
import at.qe.skeleton.models.EmployeeProfile;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.models.Userx;
import at.qe.skeleton.models.UserxRole;
import at.qe.skeleton.repositories.EmployeeProfileRepository;
import at.qe.skeleton.repositories.UserxRepository;
import at.qe.skeleton.services.AuthenticatedUserService;
import at.qe.skeleton.services.DepartmentService;
import at.qe.skeleton.services.EmployeeProfileService;
import at.qe.skeleton.services.RoomPrivacyModeService;
import at.qe.skeleton.services.RoomService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class EmployeeProfileServiceTest {

    @Mock
    private EmployeeProfileRepository employeeProfileRepository;

    @Mock
    private UserxRepository userxRepository;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private RoomService roomService;

    @Mock
    private AuthenticatedUserService authentication;

    @Mock
    private RoomPrivacyModeService roomPrivacyModeService;

    private EmployeeProfileService service;

    private EmployeeProfile profile;
    private Userx user;
    private Room room;
    private Department department;

    @BeforeEach
    void setUp() {
        service = new EmployeeProfileService(
                employeeProfileRepository,
                userxRepository,
                departmentService,
                roomService,
                authentication,
                roomPrivacyModeService
        );

        user = new Userx();
        ReflectionTestUtils.setField(user, "id", 1L);
        user.setUsername("user1");
        user.setRoles(EnumSet.of(UserxRole.EMPLOYEE));

        room = new Room();
        ReflectionTestUtils.setField(room, "id", 100L);
        room.setName("Old room");

        department = new Department();
        ReflectionTestUtils.setField(department, "id", 10L);
        department.setName("Old department");

        profile = new EmployeeProfile();
        ReflectionTestUtils.setField(profile, "id", 42L);
        profile.setUser(user);
        profile.setRoom(room);
        profile.setDepartment(department);

        user.setEmployeeProfile(profile);
    }

    @Test
    @DisplayName("gets all profiles without filters")
    void getAll_noFilters_returnsAll() {
        Mockito.when(employeeProfileRepository.findAll()).thenReturn(List.of(profile));

        List<EmployeeProfile> result = service.getAll(null, null);

        Assertions.assertThat(result).containsExactly(profile);
        Mockito.verify(employeeProfileRepository).findAll();
    }

    @Test
    @DisplayName("gets profiles by user")
    void getAll_userFilter_returnsByUser() {
        Mockito.when(employeeProfileRepository.findByUser_Id(1L)).thenReturn(List.of(profile));

        List<EmployeeProfile> result = service.getAll(1L, null);

        Assertions.assertThat(result).containsExactly(profile);
        Mockito.verify(employeeProfileRepository).findByUser_Id(1L);
    }

    @Test
    @DisplayName("gets profiles by department")
    void getAll_departmentFilter_returnsByDepartment() {
        Mockito.when(employeeProfileRepository.findByDepartment_Id(10L)).thenReturn(List.of(profile));

        List<EmployeeProfile> result = service.getAll(null, 10L);

        Assertions.assertThat(result).containsExactly(profile);
        Mockito.verify(employeeProfileRepository).findByDepartment_Id(10L);
    }

    @Test
    @DisplayName("gets profiles by user and department")
    void getAll_userAndDepartmentFilter_returnsByUserAndDepartment() {
        Mockito.when(employeeProfileRepository.findByUser_IdAndDepartment_Id(1L, 10L))
                .thenReturn(List.of(profile));

        List<EmployeeProfile> result = service.getAll(1L, 10L);

        Assertions.assertThat(result).containsExactly(profile);
        Mockito.verify(employeeProfileRepository).findByUser_IdAndDepartment_Id(1L, 10L);
    }

    @Test
    @DisplayName("gets profiles by room id")
    void getAllByRoomId_returnsProfiles() {
        Mockito.when(employeeProfileRepository.findByRoom_Id(100L)).thenReturn(List.of(profile));

        List<EmployeeProfile> result = service.getAllByRoomId(100L);

        Assertions.assertThat(result).containsExactly(profile);
        Mockito.verify(employeeProfileRepository).findByRoom_Id(100L);
    }

    @Test
    @DisplayName("gets profile by id")
    void getById_found_returnsProfile() {
        Mockito.when(employeeProfileRepository.findById(42L)).thenReturn(Optional.of(profile));

        EmployeeProfile result = service.getById(42L);

        Assertions.assertThat(result).isEqualTo(profile);
    }

    @Test
    @DisplayName("get by id throws when not found")
    void getById_notFound_throws() {
        Mockito.when(employeeProfileRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("creates profile and updates room privacy")
    void create_valid_savesAndUpdatesRoomPrivacy() {
        EmployeeProfile newProfile = new EmployeeProfile();
        newProfile.setUser(user);
        newProfile.setRoom(room);
        newProfile.setDepartment(department);

        Mockito.when(employeeProfileRepository.findByUser(user)).thenReturn(Optional.empty());
        Mockito.when(employeeProfileRepository.save(newProfile)).thenReturn(newProfile);

        EmployeeProfile result = service.create(newProfile);

        Assertions.assertThat(result).isEqualTo(newProfile);
        Mockito.verify(employeeProfileRepository).save(newProfile);
        Mockito.verify(roomPrivacyModeService).updatePrivacyModeForRoom(100L);
    }

    @Test
    @DisplayName("create throws conflict when user already has profile")
    void create_existingProfile_throwsConflict() {
        EmployeeProfile newProfile = new EmployeeProfile();
        newProfile.setUser(user);

        Mockito.when(employeeProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));

        Assertions.assertThatThrownBy(() -> service.create(newProfile))
                .isInstanceOf(ConflictException.class);

        Mockito.verify(employeeProfileRepository, Mockito.never()).save(Mockito.any());
        Mockito.verifyNoInteractions(roomPrivacyModeService);
    }

    @Test
    @DisplayName("updates department")
    void update_department_updatesDepartment() {
        Department newDept = new Department();
        ReflectionTestUtils.setField(newDept, "id", 20L);

        EmployeeProfileUpdateDTO dto = new EmployeeProfileUpdateDTO(null, 20L, null);

        Mockito.when(employeeProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        Mockito.when(departmentService.getDepartmentById(20L)).thenReturn(newDept);
        Mockito.when(employeeProfileRepository.save(profile)).thenReturn(profile);

        EmployeeProfile result = service.update(1L, dto);

        Assertions.assertThat(result.getDepartment()).isEqualTo(newDept);
        Mockito.verify(roomPrivacyModeService).updatePrivacyModeForRoom(100L);
        Mockito.verify(roomPrivacyModeService, Mockito.times(1)).updatePrivacyModeForRoom(Mockito.any());
    }

    @Test
    @DisplayName("updates room")
    void update_room_updatesRoom() {
        Room newRoom = new Room();
        ReflectionTestUtils.setField(newRoom, "id", 200L);

        EmployeeProfileUpdateDTO dto = new EmployeeProfileUpdateDTO(null, null, 200L);

        Mockito.when(employeeProfileRepository.findById(42L)).thenReturn(Optional.of(profile));
        Mockito.when(roomService.getById(200L)).thenReturn(newRoom);
        Mockito.when(employeeProfileRepository.save(profile)).thenReturn(profile);

        EmployeeProfile result = service.update(42L, dto);

        Assertions.assertThat(result.getRoom()).isEqualTo(newRoom);
        Mockito.verify(roomPrivacyModeService).updatePrivacyModeForRoom(100L);
        Mockito.verify(roomPrivacyModeService).updatePrivacyModeForRoom(200L);
    }

    @Test
    @DisplayName("updates all fields")
    void update_all_updatesAll() {
        Userx newUser = new Userx();
        ReflectionTestUtils.setField(newUser, "id", 2L);
        newUser.setUsername("user2");

        Department newDept = new Department();
        ReflectionTestUtils.setField(newDept, "id", 20L);

        Room newRoom = new Room();
        ReflectionTestUtils.setField(newRoom, "id", 200L);

        EmployeeProfileUpdateDTO dto = new EmployeeProfileUpdateDTO(2L, 20L, 200L);

        Mockito.when(employeeProfileRepository.findById(42L)).thenReturn(Optional.of(profile));
        Mockito.when(userxRepository.findById(2L)).thenReturn(Optional.of(newUser));
        Mockito.when(departmentService.getDepartmentById(20L)).thenReturn(newDept);
        Mockito.when(roomService.getById(200L)).thenReturn(newRoom);
        Mockito.when(employeeProfileRepository.save(profile)).thenReturn(profile);

        EmployeeProfile result = service.update(42L, dto);

        Assertions.assertThat(result.getUser()).isEqualTo(newUser);
        Assertions.assertThat(result.getDepartment()).isEqualTo(newDept);
        Assertions.assertThat(result.getRoom()).isEqualTo(newRoom);

        Mockito.verify(roomPrivacyModeService).updatePrivacyModeForRoom(100L);
        Mockito.verify(roomPrivacyModeService).updatePrivacyModeForRoom(200L);
    }

    @Test
    @DisplayName("empty dto saves and updates current room privacy")
    void update_empty_noChanges() {
        EmployeeProfileUpdateDTO dto = new EmployeeProfileUpdateDTO(null, null, null);

        Mockito.when(employeeProfileRepository.findById(42L)).thenReturn(Optional.of(profile));
        Mockito.when(employeeProfileRepository.save(profile)).thenReturn(profile);

        service.update(42L, dto);

        Mockito.verifyNoInteractions(userxRepository, departmentService, roomService);
        Mockito.verify(roomPrivacyModeService).updatePrivacyModeForRoom(100L);
        Mockito.verify(roomPrivacyModeService, Mockito.times(1)).updatePrivacyModeForRoom(Mockito.any());
    }

    @Test
    @DisplayName("update throws when profile not found")
    void update_notFound_throws() {
        Mockito.when(employeeProfileRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> service.update(99L, new EmployeeProfileUpdateDTO(null, null, null)))
                .isInstanceOf(NotFoundException.class);

        Mockito.verifyNoInteractions(roomPrivacyModeService);
    }

    @Test
    @DisplayName("update throws when new user not found")
    void update_userNotFound_throws() {
        EmployeeProfileUpdateDTO dto = new EmployeeProfileUpdateDTO(999L, null, null);

        Mockito.when(employeeProfileRepository.findById(42L)).thenReturn(Optional.of(profile));
        Mockito.when(userxRepository.findById(999L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> service.update(42L, dto))
                .isInstanceOf(NotFoundException.class);

        Mockito.verifyNoInteractions(roomPrivacyModeService);
    }

    @Test
    @DisplayName("deletes and clears user")
    void delete_clearsUser() {
        user.setRoles(EnumSet.of(UserxRole.EMPLOYEE, UserxRole.MANAGEMENT));

        Mockito.when(employeeProfileRepository.findById(42L)).thenReturn(Optional.of(profile));

        service.delete(42L);

        Assertions.assertThat(profile.getUser()).isNull();
        Assertions.assertThat(user.getEmployeeProfile()).isNull();
        Assertions.assertThat(user.getRoles()).containsExactly(UserxRole.MANAGEMENT);

        Mockito.verify(userxRepository).save(user);
        Mockito.verify(employeeProfileRepository).deleteById(42L);
        Mockito.verify(roomPrivacyModeService).updatePrivacyModeForRoom(100L);
    }

    @Test
    @DisplayName("deletes without user")
    void delete_noUser() {
        profile.setUser(null);

        Mockito.when(employeeProfileRepository.findById(42L)).thenReturn(Optional.of(profile));

        service.delete(42L);

        Mockito.verify(userxRepository, Mockito.never()).save(Mockito.any());
        Mockito.verify(employeeProfileRepository).deleteById(42L);
        Mockito.verify(roomPrivacyModeService).updatePrivacyModeForRoom(100L);
    }

    @Test
    @DisplayName("delete throws when profile not found")
    void delete_notFound_throws() {
        Mockito.when(employeeProfileRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(NotFoundException.class);

        Mockito.verify(employeeProfileRepository, Mockito.never()).deleteById(Mockito.any());
        Mockito.verifyNoInteractions(roomPrivacyModeService);
    }
}