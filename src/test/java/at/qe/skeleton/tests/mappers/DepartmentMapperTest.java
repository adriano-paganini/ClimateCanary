package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.DepartmentDTO;
import at.qe.skeleton.mappers.DepartmentMapper;
import at.qe.skeleton.models.*;
import at.qe.skeleton.repositories.RoomRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

@ExtendWith(MockitoExtension.class)
public class DepartmentMapperTest {

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private DepartmentMapper mapper;

    @Test
    void mapTo_shouldMapAllFields() {
        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 1L);

        Userx leader = new Userx();
        ReflectionTestUtils.setField(leader, "id", 99L);

        Department department = new Department();
        ReflectionTestUtils.setField(department, "id", 10L);
        department.setName("IT");
        department.setRooms(List.of(room));
        department.setDepartmentLeader(leader);

        DepartmentDTO dto = mapper.mapTo(department);

        Assertions.assertThat(dto.id()).isEqualTo(10L);
        Assertions.assertThat(dto.name()).isEqualTo("IT");
        Assertions.assertThat(dto.roomIds()).containsExactly(1L);
        Assertions.assertThat(dto.departmentLeadId()).isEqualTo(99L);
    }

    @Test
    void mapTo_shouldHandleNullLeader() {
        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 1L);

        Department department = new Department();
        department.setName("HR");
        department.setRooms(List.of(room));
        department.setDepartmentLeader(null);

        DepartmentDTO dto = mapper.mapTo(department);

        Assertions.assertThat(dto.departmentLeadId()).isNull();
    }

    @Test
    void mapFrom_shouldMapAndLinkRooms() {
        DepartmentDTO dto = new DepartmentDTO(
                1L,
                "IT",
                List.of(10L, 20L),
                null
        );

        Room r1 = new Room();
        Room r2 = new Room();

        Mockito.when(roomRepository.findAllByIdsAndActiveTrue(List.of(10L, 20L)))
                .thenReturn(List.of(r1, r2));

        Department result = mapper.mapFrom(dto);

        Assertions.assertThat(result.getName()).isEqualTo("IT");
        Assertions.assertThat(result.getRooms()).containsExactly(r1, r2);
        Assertions.assertThat(r1.getDepartment()).isEqualTo(result);
        Assertions.assertThat(r2.getDepartment()).isEqualTo(result);

        Mockito.verify(roomRepository).findAllByIdsAndActiveTrue(List.of(10L, 20L));
    }

    @Test
    void mapFrom_shouldHandleNullRoomIds() {
        DepartmentDTO dto = new DepartmentDTO(
                1L,
                "IT",
                null,
                null
        );

        Department result = mapper.mapFrom(dto);

        Assertions.assertThat(result.getName()).isEqualTo("IT");
        Assertions.assertThat(result.getRooms()).isEmpty();
    }
}