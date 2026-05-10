package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.RaspberryPiDTO;
import at.qe.skeleton.mappers.RaspberryPiMapper;
import at.qe.skeleton.models.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

public class RaspberryPiMapperTest {

    private final RaspberryPiMapper mapper = new RaspberryPiMapper();

    @Test
    void mapTo_shouldMapAllFields() {
        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 10L);

        SensorStation s1 = new SensorStation();
        ReflectionTestUtils.setField(s1, "id", 1L);

        SensorStation s2 = new SensorStation();
        ReflectionTestUtils.setField(s2, "id", 2L);

        RaspberryPi pi = new RaspberryPi();
        ReflectionTestUtils.setField(pi, "id", 100L);

        pi.setHostName("pi-01");
        pi.setIpAddress("192.168.0.1");
        pi.setDeviceStatus(DeviceStatus.AVAILABLE);
        pi.setRoom(room);
        pi.setSensorStations(List.of(s1, s2));

        RaspberryPiDTO dto = mapper.mapTo(pi);

        Assertions.assertThat(dto.id()).isEqualTo(100L);
        Assertions.assertThat(dto.hostName()).isEqualTo("pi-01");
        Assertions.assertThat(dto.ipAddress()).isEqualTo("192.168.0.1");
        Assertions.assertThat(dto.deviceStatus()).isEqualTo(DeviceStatus.AVAILABLE);
        Assertions.assertThat(dto.roomId()).isEqualTo(10L);
        Assertions.assertThat(dto.sensorStationIds()).containsExactly(1L, 2L);
    }

    @Test
    void mapTo_shouldHandleNullRoom_andEmptyStations() {
        RaspberryPi pi = new RaspberryPi();

        pi.setHostName("pi-02");
        pi.setIpAddress("192.168.0.2");
        pi.setDeviceStatus(DeviceStatus.OFFLINE);
        pi.setSensorStations(List.of());

        RaspberryPiDTO dto = mapper.mapTo(pi);

        Assertions.assertThat(dto.roomId()).isNull();
        Assertions.assertThat(dto.sensorStationIds()).isEmpty();
    }

    @Test
    void mapFrom_shouldThrowException() {
        RaspberryPiDTO dto = new RaspberryPiDTO(
                1L,
                "pi",
                "127.0.0.1",
                DeviceStatus.OFFLINE,
                null,
                List.of()
        );

        Assertions.assertThatThrownBy(() -> mapper.mapFrom(dto))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}