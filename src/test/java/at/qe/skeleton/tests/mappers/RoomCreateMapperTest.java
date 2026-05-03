package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.RoomCreateDTO;
import at.qe.skeleton.mappers.RoomCreateMapper;
import at.qe.skeleton.models.*;
import at.qe.skeleton.services.BuildingService;
import at.qe.skeleton.services.DepartmentService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomCreateMapperTest {

    @Mock
    private DepartmentService departmentService;

    @Mock
    private BuildingService buildingService;

    @InjectMocks
    private RoomCreateMapper mapper;

    @Test
    void mapFrom_shouldMapAllFields_andSetActiveTrue() {
        Department department = new Department();
        Building building = new Building();

        Mockito.when(departmentService.getDepartmentById(1L))
                .thenReturn(department);

        Mockito.when(buildingService.getBuildingById(2L))
                .thenReturn(building);

        RoomCreateDTO dto = new RoomCreateDTO(
                "Room A",
                RoomType.OFFICE,
                true,
                1L,
                2L
        );

        Room result = mapper.mapFrom(dto);

        Assertions.assertThat(result.getName()).isEqualTo("Room A");
        Assertions.assertThat(result.getRoomType()).isEqualTo(RoomType.OFFICE);
        Assertions.assertThat(result.getPrivacyMode()).isEqualTo(true);
        Assertions.assertThat(result.getDepartment()).isEqualTo(department);
        Assertions.assertThat(result.getBuilding()).isEqualTo(building);
        Assertions.assertThat(result.isActive()).isTrue();

        Mockito.verify(departmentService).getDepartmentById(1L);
        Mockito.verify(buildingService).getBuildingById(2L);
    }

    @Test
    void mapTo_shouldThrowException() {
        Room entity = new Room();

        Assertions.assertThatThrownBy(() -> mapper.mapTo(entity))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}