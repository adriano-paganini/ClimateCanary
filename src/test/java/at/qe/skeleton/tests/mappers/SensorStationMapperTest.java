package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.SensorStationDTO;
import at.qe.skeleton.mappers.SensorStationMapper;
import at.qe.skeleton.models.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class SensorStationMapperTest {

    private final SensorStationMapper mapper = new SensorStationMapper();

    @Test
    void mapTo_shouldMapAllFields() {
        RaspberryPi pi = new RaspberryPi();
        ReflectionTestUtils.setField(pi, "id", 10L);

        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 20L);

        SensorStation station = new SensorStation();
        ReflectionTestUtils.setField(station, "id", 1L);

        station.setName("Station A");
        station.setBleMac("AA:BB:CC:DD");
        station.setDeviceStatus(DeviceStatus.AVAILABLE);
        station.setMeasurementInterval(60);
        station.setRaspberryPi(pi);
        station.setRoom(room);

        SensorStationDTO dto = mapper.mapTo(station);

        Assertions.assertThat(dto.id()).isEqualTo(1L);
        Assertions.assertThat(dto.name()).isEqualTo("Station A");
        Assertions.assertThat(dto.bleMac()).isEqualTo("AA:BB:CC:DD");
        Assertions.assertThat(dto.deviceStatus()).isEqualTo(DeviceStatus.AVAILABLE);
        Assertions.assertThat(dto.measurementInterval()).isEqualTo(60);
        Assertions.assertThat(dto.raspberryPiId()).isEqualTo(10L);
        Assertions.assertThat(dto.roomId()).isEqualTo(20L);
    }

    @Test
    void mapTo_shouldHandleNullRelations() {
        SensorStation station = new SensorStation();
        station.setName("Station B");
        station.setBleMac("11:22:33:44");
        station.setDeviceStatus(DeviceStatus.OFFLINE);
        station.setMeasurementInterval(30);

        SensorStationDTO dto = mapper.mapTo(station);

        Assertions.assertThat(dto.raspberryPiId()).isNull();
        Assertions.assertThat(dto.roomId()).isNull();
    }

    @Test
    void mapFrom_shouldThrowException() {
        SensorStationDTO dto = new SensorStationDTO(
                1L,
                "Station",
                "AA:BB:CC:DD",
                DeviceStatus.AVAILABLE,
                60,
                null,
                null
        );

        Assertions.assertThatThrownBy(() -> mapper.mapFrom(dto))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}