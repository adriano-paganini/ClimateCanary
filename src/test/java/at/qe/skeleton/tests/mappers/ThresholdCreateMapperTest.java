package at.qe.skeleton.tests.mappers;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.ThresholdCreateDTO;
import at.qe.skeleton.mappers.ThresholdCreateMapper;
import at.qe.skeleton.models.*;
import at.qe.skeleton.repositories.ClimateHintRepository;
import at.qe.skeleton.repositories.RoomRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

@ExtendWith(MockitoExtension.class)
class ThresholdCreateMapperTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ClimateHintRepository hintRepository;

    @InjectMocks
    private ThresholdCreateMapper mapper;

    @Test
    void mapFrom_shouldMapAllFields_withHints() {
        Room room = new Room();
        ClimateHint hint1 = new ClimateHint();
        ClimateHint hint2 = new ClimateHint();

        Mockito.when(roomRepository.findById(1L))
                .thenReturn(Optional.of(room));

        Mockito.when(hintRepository.findAllById(List.of(10L, 20L)))
                .thenReturn(List.of(hint1, hint2));

        ThresholdCreateDTO dto = new ThresholdCreateDTO(
                1L,
                Metric.TEMPERATURE,
                25.0F,
                ThresholdType.UPPER,
                List.of(10L, 20L)
        );

        Threshold result = mapper.mapFrom(dto);

        Assertions.assertThat(result.getRoom()).isEqualTo(room);
        Assertions.assertThat(result.getMetric()).isEqualTo(Metric.TEMPERATURE);
        Assertions.assertThat(result.getBoundValue()).isEqualTo(25.0F);
        Assertions.assertThat(result.getThresholdType()).isEqualTo(ThresholdType.UPPER);
        Assertions.assertThat(result.isEnabled()).isTrue();
        Assertions.assertThat(result.getClimateHints())
                .containsExactlyInAnyOrder(hint1, hint2);

        Mockito.verify(roomRepository).findById(1L);
        Mockito.verify(hintRepository).findAllById(List.of(10L, 20L));
    }

    @Test
    void mapFrom_shouldHandleNullHints() {
        Room room = new Room();

        Mockito.when(roomRepository.findById(1L))
                .thenReturn(Optional.of(room));

        ThresholdCreateDTO dto = new ThresholdCreateDTO(
                1L,
                Metric.HUMIDITY,
                60.0F,
                ThresholdType.LOWER,
                null
        );

        Threshold result = mapper.mapFrom(dto);

        Assertions.assertThat(result.getRoom()).isEqualTo(room);
        Assertions.assertThat(result.getClimateHints()).isEmpty();

        Mockito.verify(roomRepository).findById(1L);
        Mockito.verifyNoInteractions(hintRepository);
    }

    @Test
    void mapFrom_shouldThrow_whenRoomNotFound() {
        Mockito.when(roomRepository.findById(99L))
                .thenReturn(Optional.empty());

        ThresholdCreateDTO dto = new ThresholdCreateDTO(
                99L,
                Metric.TEMPERATURE,
                10.0F,
                ThresholdType.LOWER,
                List.of()
        );

        Assertions.assertThatThrownBy(() -> mapper.mapFrom(dto))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void mapTo_shouldThrowException() {
        Threshold entity = new Threshold();

        Assertions.assertThatThrownBy(() -> mapper.mapTo(entity))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}