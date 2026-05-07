package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.dtos.ThresholdViolationCreateDTO;
import at.qe.skeleton.mappers.ThresholdViolationCreateMapper;
import at.qe.skeleton.models.*;
import at.qe.skeleton.services.RoomService;
import at.qe.skeleton.services.ThresholdService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class ThresholdViolationCreateMapperTest {

    @Mock
    private ThresholdService thresholdService;

    @Mock
    private RoomService roomService;

    @InjectMocks
    private ThresholdViolationCreateMapper mapper;

    @Test
    void mapFrom_shouldMapAllFields_andSetStatusActive() {
        Threshold threshold = new Threshold();
        Room room = new Room();

        Mockito.when(thresholdService.getThresholdById(1L))
                .thenReturn(threshold);

        Mockito.when(roomService.getById(2L))
                .thenReturn(room);

        LocalDateTime now = LocalDateTime.now();

        ThresholdViolationCreateDTO dto = new ThresholdViolationCreateDTO(
                Metric.TEMPERATURE,
                30.5F,
                now,
                1L,
                2L,
                null
        );

        ThresholdViolation result = mapper.mapFrom(dto);

        Assertions.assertThat(result.getMetric()).isEqualTo(Metric.TEMPERATURE);
        Assertions.assertThat(result.getValue()).isEqualTo(30.5F);
        Assertions.assertThat(result.getStartTime()).isEqualTo(now);
        Assertions.assertThat(result.getThreshold()).isEqualTo(threshold);
        Assertions.assertThat(result.getRoom()).isEqualTo(room);
        Assertions.assertThat(result.getViolationStatus()).isEqualTo(ViolationStatus.ACTIVE);

        Mockito.verify(thresholdService).getThresholdById(1L);
        Mockito.verify(roomService).getById(2L);
    }

    @Test
    void mapTo_shouldThrowException() {
        ThresholdViolation entity = new ThresholdViolation();

        Assertions.assertThatThrownBy(() -> mapper.mapTo(entity))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}