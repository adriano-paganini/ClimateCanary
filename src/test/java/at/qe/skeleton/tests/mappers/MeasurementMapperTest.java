package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.MeasurementDTO;
import at.qe.skeleton.mappers.MeasurementMapper;
import at.qe.skeleton.models.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

public class MeasurementMapperTest {

    private final MeasurementMapper mapper = new MeasurementMapper();

    @Test
    void mapTo_shouldMapAllFields() {
        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 10L);

        SensorStation station = new SensorStation();
        ReflectionTestUtils.setField(station, "id", 20L);

        ThresholdViolation v1 = new ThresholdViolation();
        ReflectionTestUtils.setField(v1, "id", 1L);

        Measurement m = new Measurement();
        ReflectionTestUtils.setField(m, "id", 100L);

        m.setTimestamp(LocalDateTime.now());
        m.setMeasurement(42.5F);
        m.setMetric(Metric.TEMPERATURE);
        m.setRoom(room);
        m.setSensorStation(station);
        m.setThresholdViolations(List.of(v1));

        MeasurementDTO dto = mapper.mapTo(m);

        Assertions.assertThat(dto.id()).isEqualTo(100L);
        Assertions.assertThat(dto.roomId()).isEqualTo(10L);
        Assertions.assertThat(dto.sensorStationId()).isEqualTo(20L);
        Assertions.assertThat(dto.thresholdViolationIds()).containsExactly(1L);
    }

    @Test
    void mapTo_shouldHandleNullRelations() {
        Measurement m = new Measurement();
        m.setThresholdViolations(List.of());

        MeasurementDTO dto = mapper.mapTo(m);

        Assertions.assertThat(dto.roomId()).isNull();
        Assertions.assertThat(dto.sensorStationId()).isNull();
        Assertions.assertThat(dto.thresholdViolationIds()).isEmpty();
    }

    @Test
    void mapFrom_shouldThrowException() {
        MeasurementDTO dto = new MeasurementDTO(
                1L, LocalDateTime.now(), 10.0F,
                Metric.TEMPERATURE, null, null, List.of()
        );

        Assertions.assertThatThrownBy(() -> mapper.mapFrom(dto))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}