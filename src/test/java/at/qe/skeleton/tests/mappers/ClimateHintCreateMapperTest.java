package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.ClimateHintCreateDTO;
import at.qe.skeleton.mappers.ClimateHintCreateMapper;
import at.qe.skeleton.models.ClimateHint;
import at.qe.skeleton.models.Metric;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ClimateHintCreateMapperTest {

    private final ClimateHintCreateMapper mapper = new ClimateHintCreateMapper();

    @Test
    void mapFrom_shouldMapAllFields() {
        ClimateHintCreateDTO dto = new ClimateHintCreateDTO(
                Metric.TEMPERATURE,
                "Open the window"
        );

        ClimateHint result = mapper.mapFrom(dto);

        Assertions.assertThat(result.getMetric()).isEqualTo(Metric.TEMPERATURE);
        Assertions.assertThat(result.getHintText()).isEqualTo("Open the window");
        Assertions.assertThat(result.getId()).isNull();
    }

    @Test
    void mapTo_shouldThrowException() {
        ClimateHint entity = new ClimateHint();

        Assertions.assertThatThrownBy(() -> mapper.mapTo(entity))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}