package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.RaspberryPiCreateDTO;
import at.qe.skeleton.mappers.RaspberryPiCreateMapper;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.services.RoomService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RaspberryPiCreateMapperTest {

    @Mock
    private RoomService roomService;

    @InjectMocks
    private RaspberryPiCreateMapper mapper;

    @Test
    void mapFrom_shouldMapAllFields_andCallService() {
        Room room = new Room();

        Mockito.when(roomService.getById(10L))
                .thenReturn(room);

        RaspberryPiCreateDTO dto = new RaspberryPiCreateDTO(
                10L,
                "pi-01"
        );

        RaspberryPi result = mapper.mapFrom(dto);

        Assertions.assertThat(result.getRoom()).isEqualTo(room);
        Assertions.assertThat(result.getHostName()).isEqualTo("pi-01");

        Mockito.verify(roomService).getById(10L);
    }

    @Test
    void mapTo_shouldThrowException() {
        RaspberryPi entity = new RaspberryPi();

        Assertions.assertThatThrownBy(() -> mapper.mapTo(entity))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}