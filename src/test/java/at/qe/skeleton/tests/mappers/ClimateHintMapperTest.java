package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.ClimateHintDTO;
import at.qe.skeleton.mappers.ClimateHintMapper;
import at.qe.skeleton.models.ClimateHint;
import at.qe.skeleton.models.Metric;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ClimateHintMapperTest {

    private final ClimateHintMapper mapper = new ClimateHintMapper();

    @Test
    void mapTo_shouldMapAllFields() {
        ClimateHint hint = new ClimateHint();
        ReflectionTestUtils.setField(hint, "id", 1L);

        hint.setMetric(Metric.TEMPERATURE);
        hint.setHintText("Keep windows closed");

        ClimateHintDTO dto = mapper.mapTo(hint);

        Assertions.assertThat(dto.id()).isEqualTo(1L);
        Assertions.assertThat(dto.metric()).isEqualTo(Metric.TEMPERATURE);
        Assertions.assertThat(dto.hintText()).isEqualTo("Keep windows closed");
    }

    @Test
    void mapFrom_shouldMapAllFields() {
        ClimateHintDTO dto = new ClimateHintDTO(
                1L,
                Metric.HUMIDITY,
                "Use a dehumidifier"
        );

        ClimateHint entity = mapper.mapFrom(dto);

        Assertions.assertThat(entity.getMetric()).isEqualTo(Metric.HUMIDITY);
        Assertions.assertThat(entity.getHintText()).isEqualTo("Use a dehumidifier");
        Assertions.assertThat(entity.getId()).isNull();
    }
}