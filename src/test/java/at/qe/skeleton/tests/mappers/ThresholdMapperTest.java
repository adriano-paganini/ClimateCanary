package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.ThresholdDTO;
import at.qe.skeleton.mappers.ThresholdMapper;
import at.qe.skeleton.models.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

public class ThresholdMapperTest {

    private final ThresholdMapper mapper = new ThresholdMapper();

    @Test
    void mapTo_shouldMapAllFields() {
        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 10L);

        ClimateHint hint1 = new ClimateHint();
        ReflectionTestUtils.setField(hint1, "id", 1L);

        ClimateHint hint2 = new ClimateHint();
        ReflectionTestUtils.setField(hint2, "id", 2L);

        Threshold threshold = new Threshold();
        ReflectionTestUtils.setField(threshold, "id", 100L);

        threshold.setRoom(room);
        threshold.setMetric(Metric.TEMPERATURE);
        threshold.setBoundValue(25.0F);
        threshold.setThresholdType(ThresholdType.UPPER);
        threshold.setClimateHints(Set.of(hint1, hint2));
        threshold.setEnabled(true);

        ThresholdDTO dto = mapper.mapTo(threshold);

        Assertions.assertThat(dto.id()).isEqualTo(100L);
        Assertions.assertThat(dto.roomId()).isEqualTo(10L);
        Assertions.assertThat(dto.metric()).isEqualTo(Metric.TEMPERATURE);
        Assertions.assertThat(dto.boundValue()).isEqualTo(25.0F);
        Assertions.assertThat(dto.thresholdType()).isEqualTo(ThresholdType.UPPER);
        Assertions.assertThat(dto.climateHintIds()).contains(1L, 2L);
        Assertions.assertThat(dto.enabled()).isTrue();
    }

    @Test
    void mapTo_shouldHandleNullRoom_andEmptyHints() {
        Threshold threshold = new Threshold();

        threshold.setMetric(Metric.HUMIDITY);
        threshold.setBoundValue(50.0F);
        threshold.setThresholdType(ThresholdType.UPPER);
        threshold.setClimateHints(Set.of());
        threshold.setEnabled(false);

        ThresholdDTO dto = mapper.mapTo(threshold);

        Assertions.assertThat(dto.roomId()).isNull();
        Assertions.assertThat(dto.climateHintIds()).isEmpty();
    }

    @Test
    void mapFrom_shouldThrowException() {
        ThresholdDTO dto = new ThresholdDTO(
                1L,
                null,
                Metric.TEMPERATURE,
                10.0F,
                ThresholdType.UPPER,
                List.of(),
                true
        );

        Assertions.assertThatThrownBy(() -> mapper.mapFrom(dto))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}