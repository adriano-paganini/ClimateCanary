package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.EmployeeProfileCreateDTO;
import at.qe.skeleton.mappers.EmployeeProfileCreateMapper;
import at.qe.skeleton.models.*;
import at.qe.skeleton.services.DepartmentService;
import at.qe.skeleton.services.RoomService;
import at.qe.skeleton.services.UserxService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeProfileCreateMapperTest {

    @Mock
    private UserxService userxService;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private RoomService roomService;

    @InjectMocks
    private EmployeeProfileCreateMapper mapper;

    @Test
    void mapFrom_shouldMapAllFields_andCallServices() {
        Userx user = new Userx();
        Department department = new Department();
        Room room = new Room();

        Mockito.when(userxService.getUserById(1L)).thenReturn(user);
        Mockito.when(departmentService.getDepartmentById(2L)).thenReturn(department);
        Mockito.when(roomService.getById(3L)).thenReturn(room);

        EmployeeProfileCreateDTO dto = new EmployeeProfileCreateDTO(
                1L,
                2L,
                3L
        );

        EmployeeProfile result = mapper.mapFrom(dto);

        Assertions.assertThat(result.getUser()).isEqualTo(user);
        Assertions.assertThat(result.getDepartment()).isEqualTo(department);
        Assertions.assertThat(result.getRoom()).isEqualTo(room);

        Mockito.verify(userxService).getUserById(1L);
        Mockito.verify(departmentService).getDepartmentById(2L);
        Mockito.verify(roomService).getById(3L);
    }

    @Test
    void mapTo_shouldThrowException() {
        EmployeeProfile entity = new EmployeeProfile();

        Assertions.assertThatThrownBy(() -> mapper.mapTo(entity))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}