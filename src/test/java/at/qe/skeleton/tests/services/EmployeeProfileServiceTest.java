package at.qe.skeleton.tests.services;

import at.qe.skeleton.common.exceptions.ConflictException;
import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.EmployeeProfileUpdateDTO;
import at.qe.skeleton.models.Department;
import at.qe.skeleton.models.EmployeeProfile;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.models.Userx;
import at.qe.skeleton.repositories.EmployeeProfileRepository;
import at.qe.skeleton.services.*;
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

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeProfileService")
class EmployeeProfileServiceTest {

    @Mock private EmployeeProfileRepository employeeProfileRepository;
    @Mock private UserxService userxService;
    @Mock private DepartmentService departmentService;
    @Mock private RoomService roomService;
    @Mock private AuthenticatedUserService authentication;

    @InjectMocks
    private EmployeeProfileService service;

    private Userx user;
    private Department department;
    private Room room;
    private EmployeeProfile profile;

    @BeforeEach
    void setUp() {
        user = new Userx();
        ReflectionTestUtils.setField(user, "id", 1L);

        department = new Department();

        room = new Room();

        profile = new EmployeeProfile();
        profile.setUser(user);
        profile.setDepartment(department);
        profile.setRoom(room);
    }


    @Test
    @DisplayName("returns all profiles when both filters are null")
    void getAll_bothNull_returnsAll() {
        Mockito.when(employeeProfileRepository.findAll()).thenReturn(List.of(profile));

        List<EmployeeProfile> result = service.getAll(null, null);

        Assertions.assertThat(result).containsExactly(profile);
        Mockito.verify(employeeProfileRepository).findAll();
    }

    @Test
    @DisplayName("filters by userId only")
    void getAll_userIdOnly_filtersByUser() {
        Mockito.when(employeeProfileRepository.findByUser_Id(1L)).thenReturn(List.of(profile));

        List<EmployeeProfile> result = service.getAll(1L, null);

        Assertions.assertThat(result).containsExactly(profile);
        Mockito.verify(employeeProfileRepository).findByUser_Id(1L);
    }

    @Test
    @DisplayName("filters by departmentId only")
    void getAll_departmentIdOnly_filtersByDepartment() {
        Mockito.when(employeeProfileRepository.findByDepartment_Id(10L)).thenReturn(List.of(profile));

        List<EmployeeProfile> result = service.getAll(null, 10L);

        Assertions.assertThat(result).containsExactly(profile);
        Mockito.verify(employeeProfileRepository).findByDepartment_Id(10L);
    }

    @Test
    @DisplayName("filters by both userId and departmentId")
    void getAll_bothProvided_filtersByBoth() {
        Mockito.when(employeeProfileRepository.findByUser_IdAndDepartment_Id(1L, 10L))
                .thenReturn(List.of(profile));

        List<EmployeeProfile> result = service.getAll(1L, 10L);

        Assertions.assertThat(result).containsExactly(profile);
        Mockito.verify(employeeProfileRepository)
                .findByUser_IdAndDepartment_Id(1L, 10L);
    }

    @Test
    @DisplayName("returns empty list Mockito.when no profiles match")
    void getAll_noMatch_returnsEmpty() {
        Mockito.when(employeeProfileRepository.findByUser_Id(99L)).thenReturn(List.of());

        List<EmployeeProfile> result = service.getAll(99L, null);

        Assertions.assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns profile for authenticated user")
    void getMyProfile_exists_returnsProfile() {
        Mockito.when(authentication.getAuthenticatedUser()).thenReturn(user);
        Mockito.when(employeeProfileRepository.getByUser_Id(1L)).thenReturn(Optional.of(profile));

        Optional<EmployeeProfile> result = service.getMyProfile();

        Assertions.assertThat(result).contains(profile);
    }

    @Test
    @DisplayName("returns empty Mockito.when no profile exists")
    void getMyProfile_missing_returnsEmpty() {
        Mockito.when(authentication.getAuthenticatedUser()).thenReturn(user);
        Mockito.when(employeeProfileRepository.getByUser_Id(1L)).thenReturn(Optional.empty());

        Optional<EmployeeProfile> result = service.getMyProfile();

        Assertions.assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("returns profile Mockito.when exists")
    void getById_exists_returnsProfile() {
        Mockito.when(employeeProfileRepository.findById(42L)).thenReturn(Optional.of(profile));

        EmployeeProfile result = service.getById(42L);

        Assertions.assertThat(result).isEqualTo(profile);
    }

    @Test
    @DisplayName("throws Mockito.when profile not found")
    void getById_notFound_throws() {
        Mockito.when(employeeProfileRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(NotFoundException.class);
    }
    
    @Test
    @DisplayName("creates profile Mockito.when none exists")
    void create_valid_saves() {
        Mockito.when(employeeProfileRepository.findByUser(user)).thenReturn(Optional.empty());
        Mockito.when(employeeProfileRepository.save(profile)).thenReturn(profile);

        EmployeeProfile result = service.create(profile);

        Assertions.assertThat(result).isEqualTo(profile);
        Mockito.verify(employeeProfileRepository).save(profile);
    }

    @Test
    @DisplayName("throws conflict Mockito.when profile exists")
    void create_existing_throws() {
        Mockito.when(employeeProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));

        Assertions.assertThatThrownBy(() -> service.create(profile))
                .isInstanceOf(ConflictException.class);

        Mockito.verify(employeeProfileRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    @DisplayName("updates user")
    void update_user_updatesUser() {
        Userx newUser = new Userx();
        ReflectionTestUtils.setField(newUser, "id", 2L);

        EmployeeProfileUpdateDTO dto = new EmployeeProfileUpdateDTO(2L, null, null);

        Mockito.when(employeeProfileRepository.findById(42L)).thenReturn(Optional.of(profile));
        Mockito.when(userxService.getUserById(2L)).thenReturn(newUser);
        Mockito.when(employeeProfileRepository.save(profile)).thenReturn(profile);

        EmployeeProfile result = service.update(42L, dto);

        Assertions.assertThat(result.getUser()).isEqualTo(newUser);
    }

    @Test
    @DisplayName("updates department")
    void update_department_updatesDepartment() {
        Department newDept = new Department();

        EmployeeProfileUpdateDTO dto = new EmployeeProfileUpdateDTO(null, 10L, null);
        Mockito.when(employeeProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        Mockito.when(departmentService.getDepartmentById(10L)).thenReturn(newDept);
        Mockito.when(employeeProfileRepository.save(profile)).thenReturn(profile);
        EmployeeProfile result = service.update(1L, dto);
        Assertions.assertThat(result.getDepartment()).isEqualTo(newDept);
    }

    @Test
    @DisplayName("updates room")
    void update_room_updatesRoom() {
        Room newRoom = new Room();

        EmployeeProfileUpdateDTO dto = new EmployeeProfileUpdateDTO(null, null, 200L);

        Mockito.when(employeeProfileRepository.findById(42L)).thenReturn(Optional.of(profile));
        Mockito.when(roomService.getById(200L)).thenReturn(newRoom);
        Mockito.when(employeeProfileRepository.save(profile)).thenReturn(profile);

        EmployeeProfile result = service.update(42L, dto);

        Assertions.assertThat(result.getRoom()).isEqualTo(newRoom);
    }

    @Test
    @DisplayName("updates all fields")
    void update_all_updatesAll() {
        Userx newUser = new Userx();
        Department newDept = new Department();
        Room newRoom = new Room();

        EmployeeProfileUpdateDTO dto = new EmployeeProfileUpdateDTO(2L, 20L, 200L);

        Mockito.when(employeeProfileRepository.findById(42L)).thenReturn(Optional.of(profile));
        Mockito.when(userxService.getUserById(2L)).thenReturn(newUser);
        Mockito.when(departmentService.getDepartmentById(20L)).thenReturn(newDept);
        Mockito.when(roomService.getById(200L)).thenReturn(newRoom);
        Mockito.when(employeeProfileRepository.save(profile)).thenReturn(profile);

        EmployeeProfile result = service.update(42L, dto);

        Assertions.assertThat(result.getUser()).isEqualTo(newUser);
        Assertions.assertThat(result.getDepartment()).isEqualTo(newDept);
        Assertions.assertThat(result.getRoom()).isEqualTo(newRoom);
    }

    @Test
    @DisplayName("empty dto does nothing")
    void update_empty_noChanges() {
        EmployeeProfileUpdateDTO dto =
                new EmployeeProfileUpdateDTO(null, null, null);

        Mockito.when(employeeProfileRepository.findById(42L)).thenReturn(Optional.of(profile));
        Mockito.when(employeeProfileRepository.save(profile)).thenReturn(profile);

        service.update(42L, dto);

        Mockito.verifyNoInteractions(userxService, departmentService, roomService);
    }

    @Test
    @DisplayName("update throws Mockito.when not found")
    void update_notFound_throws() {
        Mockito.when(employeeProfileRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> service.update(99L, new EmployeeProfileUpdateDTO(null,null,null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("deletes and clears user")
    void delete_clearsUser() {
        Mockito.when(employeeProfileRepository.findById(42L)).thenReturn(Optional.of(profile));

        service.delete(42L);

        Assertions.assertThat(profile.getUser()).isNull();
        Assertions.assertThat(user.getEmployeeProfile()).isNull();
        Mockito.verify(employeeProfileRepository).deleteById(42L);
    }

    @Test
    @DisplayName("deletes without user")
    void delete_noUser() {
        profile.setUser(null);

        Mockito.when(employeeProfileRepository.findById(42L)).thenReturn(Optional.of(profile));

        service.delete(42L);

        Mockito.verify(employeeProfileRepository).deleteById(42L);
    }

    @Test
    @DisplayName("delete throws Mockito.when not found")
    void delete_notFound() {
        Mockito.when(employeeProfileRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(NotFoundException.class);
    }
}
