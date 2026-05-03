package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.SensorStationCreateDTO;
import at.qe.skeleton.mappers.SensorStationCreateMapper;
import at.qe.skeleton.models.*;
import at.qe.skeleton.services.RaspberryPiService;
import at.qe.skeleton.services.RoomService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SensorStationCreateMapperTest {

    @Mock
    private RaspberryPiService raspberryPiService;

    @Mock
    private RoomService roomService;

    @InjectMocks
    private SensorStationCreateMapper mapper;

    @Test
    void mapFrom_shouldMapAllFields_andCallServices() {
        RaspberryPi pi = new RaspberryPi();
        Room room = new Room();

        Mockito.when(raspberryPiService.getById(1L)).thenReturn(pi);
        Mockito.when(roomService.getById(2L)).thenReturn(room);

        SensorStationCreateDTO dto = new SensorStationCreateDTO(
                "Station A",
                DeviceStatus.AVAILABLE,
                "AA:BB:CC:DD",
                60,
                1L,
                2L
        );

        SensorStation result = mapper.mapFrom(dto);

        Assertions.assertThat(result.getName()).isEqualTo("Station A");
        Assertions.assertThat(result.getDeviceStatus()).isEqualTo(DeviceStatus.AVAILABLE);
        Assertions.assertThat(result.getBleMac()).isEqualTo("AA:BB:CC:DD");
        Assertions.assertThat(result.getMeasurementInterval()).isEqualTo(60);
        Assertions.assertThat(result.getRaspberryPi()).isEqualTo(pi);
        Assertions.assertThat(result.getRoom()).isEqualTo(room);

        Mockito.verify(raspberryPiService).getById(1L);
        Mockito.verify(roomService).getById(2L);
    }

    @Test
    void mapTo_shouldThrowException() {
        SensorStation entity = new SensorStation();

        Assertions.assertThatThrownBy(() -> mapper.mapTo(entity))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}