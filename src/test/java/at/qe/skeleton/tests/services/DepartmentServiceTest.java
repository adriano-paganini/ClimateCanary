package at.qe.skeleton.tests.services;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.DepartmentUpdateDTO;
import at.qe.skeleton.models.Department;
import at.qe.skeleton.models.EmployeeProfile;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.models.Userx;
import at.qe.skeleton.models.UserxRole;
import at.qe.skeleton.repositories.DepartmentRepository;
import at.qe.skeleton.repositories.EmployeeProfileRepository;
import at.qe.skeleton.repositories.RoomRepository;
import at.qe.skeleton.services.DepartmentService;
import at.qe.skeleton.services.UserxService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserxService userxService;

    @Mock
    private EmployeeProfileRepository employeeProfileRepository;

    @InjectMocks
    private DepartmentService departmentService;

    private Department department;
    private Room room1;
    private Room room2;
    private Userx leader;
    private EmployeeProfile employeeProfile;

    @BeforeEach
    void setUp() {
        leader = new Userx();
        room1 = new Room();
        room2 = new Room();
        employeeProfile = new EmployeeProfile();
        department = new Department();

        department.setName("Engineering");
        department.setDepartmentLeader(leader);
        department.setRooms(new ArrayList<>(List.of(room1)));
        department.getEmployeeProfiles().add(employeeProfile);
        employeeProfile.setDepartment(department);
    }

    @Test
    @DisplayName("Get all departments returns all")
    void getAll_returnsAllDepartments() {
        Mockito.when(departmentRepository.findAll()).thenReturn(List.of(department));

        List<Department> result = departmentService.getAll();

        Assertions.assertThat(result).containsExactly(department);
        Mockito.verify(departmentRepository).findAll();
    }

    @Test
    @DisplayName("Get all departments empty returns empty list")
    void getAll_emptyRepository_returnsEmptyList() {
        Mockito.when(departmentRepository.findAll()).thenReturn(List.of());

        List<Department> result = departmentService.getAll();

        Assertions.assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Get department by id returns department")
    void getDepartmentById_existingId_returnsDepartment() {
        Mockito.when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        Department result = departmentService.getDepartmentById(1L);

        Assertions.assertThat(result).isEqualTo(department);
    }

    @Test
    @DisplayName("Get department by id throws when not found")
    void getDepartmentById_nonExistingId_throwsNotFoundException() {
        Mockito.when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> departmentService.getDepartmentById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Create department with valid data saves and returns")
    void create_validDepartment_savesAndReturnsDepartment() {
        Mockito.when(departmentRepository.save(department)).thenReturn(department);

        Department result = departmentService.create(department);

        Assertions.assertThat(result).isEqualTo(department);
        Mockito.verify(departmentRepository).save(department);
    }

    @Test
    @DisplayName("Create department without leader saves successfully")
    void create_departmentWithNoLeader_savesSuccessfully() {
        department.setDepartmentLeader(null);
        Mockito.when(departmentRepository.save(department)).thenReturn(department);

        Department result = departmentService.create(department);

        Assertions.assertThat(result).isEqualTo(department);
    }

    @Test
    @DisplayName("Update department with name only updates name")
    void update_nameOnly_updatesName() {
        DepartmentUpdateDTO dto = new DepartmentUpdateDTO("HR", null, null);

        Mockito.when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        Mockito.when(departmentRepository.save(department)).thenReturn(department);

        Department result = departmentService.update(1L, dto);

        Assertions.assertThat(result.getName()).isEqualTo("HR");
        Assertions.assertThat(result.getDepartmentLeader()).isEqualTo(leader);
        Mockito.verify(userxService, Mockito.never()).loadUser(Mockito.any());
        Mockito.verify(roomRepository, Mockito.never()).findAllByIdsAndActiveTrue(Mockito.any());
    }

    @Test
    @DisplayName("Update department with room ids replaces rooms and clears old associations")
    void update_roomIds_replacesRoomsAndClearsOldAssociations() {
        room1.setDepartment(department);
        DepartmentUpdateDTO dto = new DepartmentUpdateDTO(null, List.of(2L), null);

        Mockito.when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        Mockito.when(roomRepository.findAllByIdsAndActiveTrue(List.of(2L))).thenReturn(List.of(room2));
        Mockito.when(departmentRepository.save(department)).thenReturn(department);

        departmentService.update(1L, dto);

        Assertions.assertThat(room1.getDepartment()).isNull();
        Assertions.assertThat(department.getRooms()).containsExactly(room2);
        Assertions.assertThat(room2.getDepartment()).isEqualTo(department);
    }

    @Test
    @DisplayName("Update department with leader id updates leader")
    void update_departmentLeadId_updatesLeader() {
        Userx newLeader = new Userx();
        ReflectionTestUtils.setField(newLeader, "id", 5L);
        ReflectionTestUtils.setField(department, "id", 1L);
        leader.setRoles(new HashSet<>(List.of(UserxRole.DEPARTMENT_LEAD)));

        DepartmentUpdateDTO dto = new DepartmentUpdateDTO(null, null, 5L);

        Mockito.when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        Mockito.when(userxService.loadUser(5L)).thenReturn(Optional.of(newLeader));
        Mockito.when(departmentRepository.findByDepartmentLeader(leader)).thenReturn(List.of(department));
        Mockito.when(departmentRepository.save(department)).thenReturn(department);

        departmentService.update(1L, dto);

        Assertions.assertThat(department.getDepartmentLeader()).isEqualTo(newLeader);
        Assertions.assertThat(newLeader.getRoles()).contains(UserxRole.DEPARTMENT_LEAD);
        Assertions.assertThat(leader.getRoles()).doesNotContain(UserxRole.DEPARTMENT_LEAD);
        Mockito.verify(userxService).saveUser(newLeader);
        Mockito.verify(userxService).saveUser(leader);
    }

    @Test
    @DisplayName("Update department keeps role on previous leader when they lead another department")
    void update_departmentLeadId_keepsRoleOnPreviousLeaderWhenLeadingAnotherDepartment() {
        Userx newLeader = new Userx();
        ReflectionTestUtils.setField(newLeader, "id", 5L);
        ReflectionTestUtils.setField(department, "id", 1L);
        leader.setRoles(new HashSet<>(List.of(UserxRole.DEPARTMENT_LEAD)));

        Department otherDepartment = new Department();
        ReflectionTestUtils.setField(otherDepartment, "id", 2L);
        otherDepartment.setDepartmentLeader(leader);

        DepartmentUpdateDTO dto = new DepartmentUpdateDTO(null, null, 5L);

        Mockito.when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        Mockito.when(userxService.loadUser(5L)).thenReturn(Optional.of(newLeader));
        Mockito.when(departmentRepository.findByDepartmentLeader(leader)).thenReturn(List.of(department, otherDepartment));
        Mockito.when(departmentRepository.save(department)).thenReturn(department);

        departmentService.update(1L, dto);

        Assertions.assertThat(department.getDepartmentLeader()).isEqualTo(newLeader);
        Assertions.assertThat(newLeader.getRoles()).contains(UserxRole.DEPARTMENT_LEAD);
        Assertions.assertThat(leader.getRoles()).contains(UserxRole.DEPARTMENT_LEAD);
        Mockito.verify(userxService).saveUser(newLeader);
        Mockito.verify(userxService, Mockito.never()).saveUser(leader);
    }

    @Test
    @DisplayName("Update department with non existing leader throws not found")
    void update_departmentLeadId_nonExistingUser_throwsNotFoundException() {
        DepartmentUpdateDTO dto = new DepartmentUpdateDTO(null, null, 99L);

        Mockito.when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        Mockito.when(userxService.loadUser(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> departmentService.update(1L, dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");

        Mockito.verify(departmentRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    @DisplayName("Update department with all fields updates all")
    void update_allFieldsProvided_updatesAll() {
        Userx newLeader = new Userx();
        ReflectionTestUtils.setField(newLeader, "id", 7L);

        DepartmentUpdateDTO dto = new DepartmentUpdateDTO(
                "Finance",
                List.of(2L),
                7L
        );

        Mockito.when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        Mockito.when(roomRepository.findAllByIdsAndActiveTrue(List.of(2L))).thenReturn(List.of(room2));
        Mockito.when(userxService.loadUser(7L)).thenReturn(Optional.of(newLeader));
        Mockito.when(departmentRepository.save(department)).thenReturn(department);
        Department result = departmentService.update(1L, dto);

        Assertions.assertThat(result.getName()).isEqualTo("Finance");
        Assertions.assertThat(result.getRooms()).containsExactly(room2);
        Assertions.assertThat(result.getDepartmentLeader()).isEqualTo(newLeader);
    }

    @Test
    @DisplayName("Update department with empty dto does not change anything")
    void update_emptyDto_nothingChanged() {
        DepartmentUpdateDTO dto = new DepartmentUpdateDTO(null, null, null);

        Mockito.when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        Mockito.when(departmentRepository.save(department)).thenReturn(department);

        String originalName = department.getName();

        departmentService.update(10L, dto);

        Assertions.assertThat(department.getName()).isEqualTo(originalName);
        Assertions.assertThat(department.getDepartmentLeader()).isEqualTo(leader);
    }

    @Test
    @DisplayName("Update department with non existing id throws not found")
    void update_nonExistingId_throwsNotFoundException() {
        DepartmentUpdateDTO dto = new DepartmentUpdateDTO("HR", null, null);
        Mockito.when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> departmentService.update(99L, dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");

        Mockito.verify(departmentRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    @DisplayName("Delete department clears rooms and employees and deletes")
    void delete_existingDepartment_clearsRoomsAndEmployeesAndDeletes() {
        room1.setDepartment(department);
        employeeProfile.setDepartment(department);

        Mockito.when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        departmentService.delete(1L);

        Assertions.assertThat(room1.getDepartment()).isNull();
        Mockito.verify(roomRepository).save(room1);

        Assertions.assertThat(employeeProfile.getDepartment()).isNull();
        Mockito.verify(employeeProfileRepository).save(employeeProfile);

        Assertions.assertThat(department.getRooms()).isEmpty();
        Assertions.assertThat(department.getEmployeeProfiles()).isEmpty();

        Mockito.verify(departmentRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Delete department with no rooms or employees deletes successfully")
    void delete_departmentWithNoRoomsOrEmployees_deletesSuccessfully() {
        department.setRooms(new ArrayList<>());
        department.getEmployeeProfiles().clear();

        Mockito.when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        departmentService.delete(1L);

        Mockito.verify(roomRepository, Mockito.never()).save(Mockito.any());
        Mockito.verify(employeeProfileRepository, Mockito.never()).save(Mockito.any());
        Mockito.verify(departmentRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Delete department with multiple rooms clears all rooms")
    void delete_departmentWithMultipleRooms_clearsAllRooms() {
        room2.setDepartment(department);
        department.setRooms(new ArrayList<>(List.of(room1, room2)));

        Mockito.when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

        departmentService.delete(1L);

        Assertions.assertThat(room1.getDepartment()).isNull();
        Assertions.assertThat(room2.getDepartment()).isNull();
        Mockito.verify(roomRepository).save(room1);
        Mockito.verify(roomRepository).save(room2);
        Mockito.verify(departmentRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Delete department with non existing id throws not found")
    void delete_nonExistingId_throwsNotFoundException() {
        Mockito.when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> departmentService.delete(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");

        Mockito.verify(departmentRepository, Mockito.never()).deleteById(Mockito.any());
        Mockito.verify(roomRepository, Mockito.never()).save(Mockito.any());
        Mockito.verify(employeeProfileRepository, Mockito.never()).save(Mockito.any());
    }
}
