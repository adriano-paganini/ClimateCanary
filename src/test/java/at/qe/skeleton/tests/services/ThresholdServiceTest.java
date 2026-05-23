package at.qe.skeleton.tests.services;

import at.qe.skeleton.common.exceptions.ConflictException;
import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.ThresholdCreateDTO;
import at.qe.skeleton.dtos.ThresholdUpdateDTO;
import at.qe.skeleton.mappers.ClimateHintMapper;
import at.qe.skeleton.mappers.ThresholdMapper;
import at.qe.skeleton.models.*;
import at.qe.skeleton.repositories.ClimateHintRepository;
import at.qe.skeleton.repositories.ThresholdRepository;
import at.qe.skeleton.services.RaspberryPiServerService;
import at.qe.skeleton.services.RoomService;
import at.qe.skeleton.services.ThresholdService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ThresholdServiceTest {

    @Mock
    private ThresholdRepository thresholdRepository;

    @Mock
    private RoomService roomService;

    @Mock
    private ClimateHintRepository climateHintRepository;

    @Mock
    private ThresholdMapper thresholdMapper;

    @Mock
    private RaspberryPiServerService raspberryPiServerService;

    @Mock
    private ClimateHintMapper climateHintMapper;

    @InjectMocks
    private ThresholdService thresholdService;

    private Room room;
    private Threshold threshold;
    private ClimateHint climateHint;

    @BeforeEach
    void setUp() {
        room = new Room();
        ReflectionTestUtils.setField(room, "id", 1L);

        climateHint = new ClimateHint();
        ReflectionTestUtils.setField(climateHint, "id", 10L);
        climateHint.setMetric(Metric.TEMPERATURE);

        threshold = new Threshold();
        ReflectionTestUtils.setField(threshold, "id", 100L);
        threshold.setMetric(Metric.TEMPERATURE);
        threshold.setBoundValue(25.0F);
        threshold.setThresholdType(ThresholdType.UPPER);
        threshold.setEnabled(true);
        threshold.setRoom(room);
        threshold.setClimateHints(new HashSet<>());
    }

    @Test
    @DisplayName("getAll returns thresholds filtered by roomId and metric")
    void getAll_withRoomAndMetric() {
        Mockito.when(thresholdRepository.findByRoom_IdAndMetric(1L, Metric.TEMPERATURE))
                .thenReturn(List.of(threshold));

        List<Threshold> result = thresholdService.getAll(1L, Metric.TEMPERATURE);

        Assertions.assertThat(result).containsExactly(threshold);
    }

    @Test
    @DisplayName("getAll returns thresholds filtered by roomId only")
    void getAll_withRoomOnly() {
        Mockito.when(thresholdRepository.findByRoom_Id(1L))
                .thenReturn(List.of(threshold));

        List<Threshold> result = thresholdService.getAll(1L, null);

        Assertions.assertThat(result).containsExactly(threshold);
    }

    @Test
    @DisplayName("getAll returns thresholds filtered by metric only")
    void getAll_withMetricOnly() {
        Mockito.when(thresholdRepository.findByMetric(Metric.TEMPERATURE))
                .thenReturn(List.of(threshold));

        List<Threshold> result = thresholdService.getAll(null, Metric.TEMPERATURE);

        Assertions.assertThat(result).containsExactly(threshold);
    }

    @Test
    @DisplayName("getAll returns all thresholds when no filters are provided")
    void getAll_noFilters() {
        Mockito.when(thresholdRepository.findAll())
                .thenReturn(List.of(threshold));

        List<Threshold> result = thresholdService.getAll(null, null);

        Assertions.assertThat(result).containsExactly(threshold);
    }


    @Test
    @DisplayName("getThresholdById returns threshold when found")
    void getById_found() {
        Mockito.when(thresholdRepository.findById(100L))
                .thenReturn(Optional.of(threshold));

        Threshold result = thresholdService.getThresholdById(100L);

        Assertions.assertThat(result).isEqualTo(threshold);
    }

    @Test
    @DisplayName("getThresholdById throws NotFoundException when not found")
    void getById_notFound() {
        Mockito.when(thresholdRepository.findById(99L))
                .thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> thresholdService.getThresholdById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("create saves threshold with climate hints")
    void create_withClimateHints() {
        ThresholdCreateDTO dto = new ThresholdCreateDTO(
                1L, Metric.TEMPERATURE, 25.0F, ThresholdType.UPPER, List.of(10L));

        Mockito.when(roomService.getById(1L)).thenReturn(room);
        Mockito.when(climateHintRepository.findAllById(List.of(10L)))
                .thenReturn(List.of(climateHint));
        Mockito.when(thresholdRepository.save(Mockito.any()))
                .thenAnswer(inv -> inv.getArgument(0));

        Threshold result = thresholdService.create(dto);

        Assertions.assertThat(result.getMetric()).isEqualTo(Metric.TEMPERATURE);
        Assertions.assertThat(result.getBoundValue()).isEqualTo(25.0F);
        Assertions.assertThat(result.getThresholdType()).isEqualTo(ThresholdType.UPPER);
        Assertions.assertThat(result.getClimateHints()).containsExactly(climateHint);
        Assertions.assertThat(result.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("create rejects duplicate threshold for same room, metric and type")
    void create_duplicateRoomMetricAndType_throwsConflictException() {
        ThresholdCreateDTO dto = new ThresholdCreateDTO(
                1L, Metric.TEMPERATURE, 25.0F, ThresholdType.UPPER, null);

        Mockito.when(thresholdRepository.existsByRoomIdAndMetricAndThresholdType(1L, Metric.TEMPERATURE, ThresholdType.UPPER))
                .thenReturn(true);

        Assertions.assertThatThrownBy(() -> thresholdService.create(dto))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Threshold already exists for room 1 and metric TEMPERATURE and threshold type UPPER");

        Mockito.verify(thresholdRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    @DisplayName("create saves threshold without climate hints when none provided")
    void create_withoutClimateHints() {
        ThresholdCreateDTO dto = new ThresholdCreateDTO(
                1L, Metric.HUMIDITY, 60.0F, ThresholdType.LOWER, null);

        Mockito.when(roomService.getById(1L)).thenReturn(room);
        Mockito.when(thresholdRepository.save(Mockito.any()))
                .thenAnswer(inv -> inv.getArgument(0));

        Threshold result = thresholdService.create(dto);

        Assertions.assertThat(result.getClimateHints()).isEmpty();
    }

    @Test
    @DisplayName("update modifies all fields when provided")
    void update_allFields() {
        Room newRoom = new Room();
        ReflectionTestUtils.setField(newRoom, "id", 2L);
        climateHint.setMetric(Metric.HUMIDITY);

        List<Long> hintIds = List.of(10L);

        ThresholdUpdateDTO dto = new ThresholdUpdateDTO(
                2L,
                Metric.HUMIDITY,
                50.0F,
                ThresholdType.LOWER,
                hintIds,
                false
        );

        Mockito.when(thresholdRepository.findById(100L))
                .thenReturn(Optional.of(threshold));

        Mockito.when(roomService.getById(2L))
                .thenReturn(newRoom);

        Mockito.when(climateHintRepository.findAllById(hintIds))
                .thenReturn(List.of(climateHint));

        Mockito.when(thresholdRepository.save(threshold))
                .thenReturn(threshold);

        Threshold result = thresholdService.update(100L, dto);

        Assertions.assertThat(result.getMetric()).isEqualTo(Metric.HUMIDITY);
        Assertions.assertThat(result.getBoundValue()).isEqualTo(50.0F);
        Assertions.assertThat(result.getThresholdType()).isEqualTo(ThresholdType.LOWER);
        Assertions.assertThat(result.getRoom()).isEqualTo(newRoom);
        Assertions.assertThat(result.isEnabled()).isFalse();
        Assertions.assertThat(result.getClimateHints()).containsExactly(climateHint);
    }

    @Test
    @DisplayName("update rejects duplicate threshold for effective room, metric and type")
    void update_duplicateRoomMetricAndType_throwsConflictException() {
        ThresholdUpdateDTO dto = new ThresholdUpdateDTO(
                null,
                Metric.HUMIDITY,
                50.0F,
                ThresholdType.LOWER,
                null,
                null
        );

        Mockito.when(thresholdRepository.findById(100L))
                .thenReturn(Optional.of(threshold));
        Mockito.when(thresholdRepository.existsByRoomIdAndMetricAndThresholdTypeAndIdNot(1L, Metric.HUMIDITY, ThresholdType.LOWER, 100L))
                .thenReturn(true);

        Assertions.assertThatThrownBy(() -> thresholdService.update(100L, dto))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Threshold already exists for room 1 and metric HUMIDITY and threshold type LOWER");

        Mockito.verify(thresholdRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    @DisplayName("update keeps existing values when DTO fields are null")
    void update_partial() {
        ThresholdUpdateDTO dto = new ThresholdUpdateDTO(
                null, null, null, null, null, null);

        Mockito.when(thresholdRepository.findById(100L))
                .thenReturn(Optional.of(threshold));
        Mockito.when(thresholdRepository.save(threshold)).thenReturn(threshold);

        Threshold result = thresholdService.update(100L, dto);

        Assertions.assertThat(result.getMetric()).isEqualTo(Metric.TEMPERATURE);
        Assertions.assertThat(result.getBoundValue()).isEqualTo(25.0F);
        Assertions.assertThat(result.getThresholdType()).isEqualTo(ThresholdType.UPPER);
    }

    @Test
    @DisplayName("delete removes threshold and clears climate hints")
    void delete_success() {
        threshold.getClimateHints().add(climateHint);

        Mockito.when(thresholdRepository.findById(100L))
                .thenReturn(Optional.of(threshold));

        thresholdService.delete(100L);

        Assertions.assertThat(threshold.getClimateHints()).isEmpty();
        Mockito.verify(thresholdRepository).delete(threshold);
    }

    @Test
    @DisplayName("delete throws ConflictException when violations exist")
    void delete_withViolations() {
        threshold.getViolations().add(new ThresholdViolation());

        Mockito.when(thresholdRepository.findById(100L))
                .thenReturn(Optional.of(threshold));

        Assertions.assertThatThrownBy(() -> thresholdService.delete(100L))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("delete throws NotFoundException when threshold does not exist")
    void delete_notFound() {
        Mockito.when(thresholdRepository.findById(404L))
                .thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> thresholdService.delete(404L))
                .isInstanceOf(NotFoundException.class);
    }
}
