package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.DepartmentCreateDTO;
import at.qe.skeleton.mappers.DepartmentCreateMapper;
import at.qe.skeleton.models.*;
import at.qe.skeleton.repositories.RoomRepository;
import at.qe.skeleton.services.UserxService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class DepartmentCreateMapperTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserxService userxService;

    @InjectMocks
    private DepartmentCreateMapper mapper;

    @Test
    void mapFrom_shouldMapAllFields_andLinkRooms() {
        Userx leader = new Userx();

        Room r1 = new Room();
        Room r2 = new Room();

        Mockito.when(userxService.loadUser(10L))
                .thenReturn(Optional.of(leader));

        Mockito.when(roomRepository.findAllByIdsAndActiveTrue(List.of(1L, 2L)))
                .thenReturn(List.of(r1, r2));

        DepartmentCreateDTO dto = new DepartmentCreateDTO(
                "IT",
                List.of(1L, 2L),
                10L
        );

        Department result = mapper.mapFrom(dto);

        Assertions.assertThat(result.getName()).isEqualTo("IT");
        Assertions.assertThat(result.getDepartmentLeader()).isEqualTo(leader);
        Assertions.assertThat(result.getRooms()).containsExactly(r1, r2);

        Assertions.assertThat(r1.getDepartment()).isEqualTo(result);
        Assertions.assertThat(r2.getDepartment()).isEqualTo(result);

        Mockito.verify(userxService).loadUser(10L);
        Mockito.verify(roomRepository).findAllByIdsAndActiveTrue(List.of(1L, 2L));
    }

    @Test
    void mapFrom_shouldHandleNullRoomIds() {
        Userx leader = new Userx();

        Mockito.when(userxService.loadUser(10L))
                .thenReturn(Optional.of(leader));

        DepartmentCreateDTO dto = new DepartmentCreateDTO(
                "HR",
                null,
                10L
        );

        Department result = mapper.mapFrom(dto);

        Assertions.assertThat(result.getName()).isEqualTo("HR");
        Assertions.assertThat(result.getDepartmentLeader()).isEqualTo(leader);
        Assertions.assertThat(result.getRooms()).isEmpty();

        Mockito.verifyNoInteractions(roomRepository);
    }

    @Test
    void mapFrom_shouldThrowException_whenUserNotFound() {
        Mockito.when(userxService.loadUser(99L))
                .thenReturn(Optional.empty());

        DepartmentCreateDTO dto = new DepartmentCreateDTO(
                "IT",
                List.of(1L),
                99L

        );

        Assertions.assertThatThrownBy(() -> mapper.mapFrom(dto))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void mapTo_shouldThrowException() {
        Department entity = new Department();

        Assertions.assertThatThrownBy(() -> mapper.mapTo(entity))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}