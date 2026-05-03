package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.ThresholdViolationDTO;
import at.qe.skeleton.mappers.ThresholdViolationMapper;
import at.qe.skeleton.models.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

public class ThresholdViolationMapperTest {

    private final ThresholdViolationMapper mapper = new ThresholdViolationMapper();

    @Test
    void mapTo_shouldMapAllFields() {
        Threshold threshold = new Threshold();
        ReflectionTestUtils.setField(threshold, "id", 10L);

        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 20L);

        ThresholdViolation v = new ThresholdViolation();
        ReflectionTestUtils.setField(v, "id", 1L);

        v.setMetric(Metric.TEMPERATURE);
        v.setValue(42.0F);
        v.setViolationStatus(ViolationStatus.ACTIVE);
        v.setStartTime(LocalDateTime.now().minusHours(1));
        v.setEndTime(LocalDateTime.now());
        v.setThreshold(threshold);
        v.setRoom(room);

        ThresholdViolationDTO dto = mapper.mapTo(v);

        Assertions.assertThat(dto.id()).isEqualTo(1L);
        Assertions.assertThat(dto.metric()).isEqualTo(Metric.TEMPERATURE);
        Assertions.assertThat(dto.value()).isEqualTo(42.0F);
        Assertions.assertThat(dto.thresholdId()).isEqualTo(10L);
        Assertions.assertThat(dto.roomId()).isEqualTo(20L);
        Assertions.assertThat(dto.measurementIds()).isEmpty();
    }

    @Test
    void mapTo_shouldHandleNullRelations() {
        ThresholdViolation v = new ThresholdViolation();
        v.setMetric(Metric.HUMIDITY);
        v.setValue(10.0F);
        v.setViolationStatus(ViolationStatus.RESOLVED);
        v.setStartTime(LocalDateTime.now());

        ThresholdViolationDTO dto = mapper.mapTo(v);

        Assertions.assertThat(dto.thresholdId()).isNull();
        Assertions.assertThat(dto.roomId()).isNull();
        Assertions.assertThat(dto.measurementIds()).isEmpty();
    }

    @Test
    void mapTo_shouldReturnNull_whenEntityIsNull() {
        ThresholdViolationDTO dto = mapper.mapTo(null);

        Assertions.assertThat(dto).isNull();
    }

    @Test
    void mapFrom_shouldThrowException() {
        ThresholdViolationDTO dto = new ThresholdViolationDTO(
                1L,
                Metric.TEMPERATURE,
                10.0F,
                ViolationStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                null,
                List.of()
        );

        Assertions.assertThatThrownBy(() -> mapper.mapFrom(dto)).isInstanceOf(UnsupportedOperationException.class);
    }
}