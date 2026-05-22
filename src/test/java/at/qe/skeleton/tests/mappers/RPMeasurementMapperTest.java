package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.RPMeasurementDTO;
import at.qe.skeleton.mappers.RPMeasurementMapper;
import at.qe.skeleton.models.Measurement;
import at.qe.skeleton.models.Metric;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.models.SensorStation;
import at.qe.skeleton.services.RoomService;
import at.qe.skeleton.services.SensorStationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RPMeasurementMapperTest {

    @Mock
    private RoomService roomService;

    @Mock
    private SensorStationService sensorStationService;

    @InjectMocks
    private RPMeasurementMapper mapper;

    @Test
    void mapTo_shouldThrowUnsupportedOperationException() {
        List<Measurement> measurements = new ArrayList<>();
        assertThrows(UnsupportedOperationException.class, () -> mapper.mapTo(measurements));
    }

    @Test
    void mapFrom_parsesIsoTimestampForAllMeasurements() {
        String timestampString = "2026-05-18T12:05:30.123";
        LocalDateTime timestamp = LocalDateTime.parse(timestampString);
        Room room = new Room();
        SensorStation sensorStation = new SensorStation();

        when(roomService.getById(10L)).thenReturn(room);
        when(sensorStationService.getByIdInternal(20L)).thenReturn(sensorStation);

        RPMeasurementDTO dto = new RPMeasurementDTO(
                timestampString,
                21.5F,
                45.0F,
                1013.2F,
                80.0F,
                10L,
                20L
        );

        List<Measurement> measurements = mapper.mapFrom(dto);

        assertThat(measurements)
                .hasSize(4)
                .extracting(Measurement::getTimestamp)
                .containsOnly(timestamp);
        assertThat(measurements)
                .extracting(Measurement::getMetric)
                .containsExactly(Metric.TEMPERATURE, Metric.HUMIDITY, Metric.PRESSURE, Metric.IAQ);
        assertThat(measurements)
                .allSatisfy(measurement -> {
                    assertThat(measurement.getRoom()).isSameAs(room);
                    assertThat(measurement.getSensorStation()).isSameAs(sensorStation);
                });
    }
}
