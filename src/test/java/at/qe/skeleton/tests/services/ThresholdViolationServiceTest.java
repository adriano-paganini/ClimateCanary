package at.qe.skeleton.tests.services;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.ThresholdViolationCreateDTO;
import at.qe.skeleton.dtos.ThresholdViolationUpdateDTO;
import at.qe.skeleton.dtos.ViolationActiveDTO;
import at.qe.skeleton.dtos.ViolationResolvedDTO;
import at.qe.skeleton.mappers.ThresholdViolationCreateMapper;
import at.qe.skeleton.models.*;
import at.qe.skeleton.repositories.ThresholdRepository;
import at.qe.skeleton.repositories.ThresholdViolationRepository;
import at.qe.skeleton.services.MeasurementService;
import at.qe.skeleton.services.PiRequestResult;
import at.qe.skeleton.services.RaspberryPiServerService;
import at.qe.skeleton.services.RaspberryPiService;
import at.qe.skeleton.services.RoomService;
import at.qe.skeleton.services.ThresholdService;
import at.qe.skeleton.services.ThresholdViolationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThresholdViolationServiceTest {

    @Mock
    private ThresholdViolationRepository thresholdViolationRepository;

    @Mock
    private RoomService roomService;

    @Mock
    private ThresholdService thresholdService;

    @Mock
    private ThresholdViolationCreateMapper thresholdViolationCreateMapper;

    @Mock
    private ThresholdRepository thresholdRepository;

    @Mock
    private MeasurementService measurementService;

    @Mock
    private RaspberryPiService raspberryPiService;

    @Mock
    private RaspberryPiServerService raspberryPiServerService;

    @InjectMocks
    private ThresholdViolationService thresholdViolationService;

    private ThresholdViolation violation;

    @BeforeEach
    void setUp() {
        violation = new ThresholdViolation();
        violation.setMetric(Metric.TEMPERATURE);
        violation.setValue(99.0F);
        violation.setViolationStatus(ViolationStatus.ACTIVE);
        violation.setStartTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("findAll returns all violations when no filters are provided")
    void findAll_noFilters_returnsAllViolations() {
        when(thresholdViolationRepository.findAll()).thenReturn(List.of(violation));

        List<ThresholdViolation> result = thresholdViolationService.findAll(null, null, null);

        assertThat(result).containsExactly(violation);
        verify(thresholdViolationRepository).findAll();
        verify(thresholdViolationRepository, never()).search(any(), any(), any());
    }

    @Test
    @DisplayName("findAll uses search when status filter is provided")
    void findAll_withStatusFilter_callsSearch() {
        when(thresholdViolationRepository.search(ViolationStatus.ACTIVE, null, null))
                .thenReturn(List.of(violation));

        List<ThresholdViolation> result =
                thresholdViolationService.findAll(ViolationStatus.ACTIVE, null, null);

        assertThat(result).containsExactly(violation);
        verify(thresholdViolationRepository)
                .search(ViolationStatus.ACTIVE, null, null);
    }

    @Test
    @DisplayName("findAll uses search when roomId filter is provided")
    void findAll_withRoomIdFilter_callsSearch() {
        when(thresholdViolationRepository.search(null, 10L, null))
                .thenReturn(List.of(violation));

        List<ThresholdViolation> result =
                thresholdViolationService.findAll(null, 10L, null);

        assertThat(result).containsExactly(violation);
        verify(thresholdViolationRepository)
                .search(null, 10L, null);
    }

    @Test
    @DisplayName("findAll uses search when departmentId filter is provided")
    void findAll_withDepartmentIdFilter_callsSearch() {
        when(thresholdViolationRepository.search(null, null, 5L))
                .thenReturn(List.of(violation));

        List<ThresholdViolation> result =
                thresholdViolationService.findAll(null, null, 5L);

        assertThat(result).containsExactly(violation);
        verify(thresholdViolationRepository)
                .search(null, null, 5L);
    }

    @Test
    @DisplayName("findAll with all filters calls search")
    void findAll_withAllFilters_callsSearch() {
        when(thresholdViolationRepository.search(ViolationStatus.ACTIVE, 10L, 5L))
                .thenReturn(List.of(violation));

        List<ThresholdViolation> result =
                thresholdViolationService.findAll(ViolationStatus.ACTIVE, 10L, 5L);

        assertThat(result).containsExactly(violation);
    }

    @Test
    @DisplayName("findAll returns empty list when repository is empty")
    void findAll_noFilters_emptyRepository_returnsEmptyList() {
        when(thresholdViolationRepository.findAll()).thenReturn(List.of());

        List<ThresholdViolation> result =
                thresholdViolationService.findAll(null, null, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findById returns violation when it exists")
    void findById_existingId_returnsViolation() {
        when(thresholdViolationRepository.findById(1L))
                .thenReturn(Optional.of(violation));

        ThresholdViolation result = thresholdViolationService.findById(1L);

        assertThat(result).isEqualTo(violation);
    }

    @Test
    @DisplayName("findById throws NotFoundException when violation does not exist")
    void findById_nonExistingId_throwsNotFoundException() {
        when(thresholdViolationRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                thresholdViolationService.findById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("create saves and returns violation")
    void create_validDto_savesAndReturnsViolation() {
        ThresholdViolationCreateDTO dto = mock(ThresholdViolationCreateDTO.class);

        when(thresholdViolationCreateMapper.mapFrom(dto)).thenReturn(violation);
        when(thresholdViolationRepository.save(violation)).thenReturn(violation);

        ThresholdViolation result = thresholdViolationService.create(dto);

        assertThat(result).isEqualTo(violation);
        verify(thresholdViolationCreateMapper).mapFrom(dto);
        verify(thresholdViolationRepository).save(violation);
    }

    @Test
    @DisplayName("create does not throw when threshold and room are null")
    void create_savedViolationWithNullThresholdAndRoom_doesNotThrow() {
        ThresholdViolationCreateDTO dto = mock(ThresholdViolationCreateDTO.class);

        violation.setThreshold(null);
        violation.setRoom(null);

        when(thresholdViolationCreateMapper.mapFrom(dto)).thenReturn(violation);
        when(thresholdViolationRepository.save(violation)).thenReturn(violation);

        assertThatCode(() ->
                thresholdViolationService.create(dto)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("create from Raspberry Pi parses ISO start timestamp")
    void createFromRaspberryPi_parsesIsoStartTimestamp() {
        String startTimeString = "2026-05-18T14:30:15.123";
        LocalDateTime parsedStartTime = LocalDateTime.parse(startTimeString);
        ViolationActiveDTO dto = new ViolationActiveDTO(
                Metric.TEMPERATURE,
                10L,
                startTimeString,
                31.5F
        );

        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 10L);

        RaspberryPi raspberryPi = new RaspberryPi();
        ReflectionTestUtils.setField(raspberryPi, "id", 5L);
        raspberryPi.setRoom(room);

        Threshold threshold = new Threshold();
        ReflectionTestUtils.setField(threshold, "id", 7L);
        threshold.setThresholdType(ThresholdType.UPPER);
        threshold.setBoundValue(30.0F);

        Measurement measurement = new Measurement();
        ReflectionTestUtils.setField(measurement, "id", 99L);

        when(raspberryPiService.getById(5L)).thenReturn(raspberryPi);
        when(thresholdRepository.findByRoom_IdAndMetric(10L, Metric.TEMPERATURE))
                .thenReturn(List.of(threshold));
        when(measurementService.getFiltered(eq(10L), eq(Metric.TEMPERATURE), eq(parsedStartTime), any(LocalDateTime.class)))
                .thenReturn(List.of(measurement));
        when(thresholdViolationCreateMapper.mapFrom(any(ThresholdViolationCreateDTO.class)))
                .thenReturn(violation);
        when(thresholdViolationRepository.save(violation)).thenReturn(violation);

        thresholdViolationService.create(5L, dto);

        ArgumentCaptor<ThresholdViolationCreateDTO> captor =
                ArgumentCaptor.forClass(ThresholdViolationCreateDTO.class);
        verify(thresholdViolationCreateMapper).mapFrom(captor.capture());

        ThresholdViolationCreateDTO createDTO = captor.getValue();
        assertThat(createDTO.startTime()).isEqualTo(parsedStartTime);
        assertThat(createDTO.thresholdId()).isEqualTo(7L);
        assertThat(createDTO.measurementIds()).containsExactly(99L);
    }

    @Test
    @DisplayName("update modifies metric only")
    void update_metricOnly_updatesMetric() {
        ThresholdViolationUpdateDTO dto =
                new ThresholdViolationUpdateDTO(
                        Metric.HUMIDITY, null, null, null, null, null, null, null);

        when(thresholdViolationRepository.findById(1L)).thenReturn(Optional.of(violation));
        when(thresholdViolationRepository.save(violation)).thenReturn(violation);

        ThresholdViolation result = thresholdViolationService.update(1L, dto);

        assertThat(result.getMetric()).isEqualTo(Metric.HUMIDITY);
        verify(thresholdViolationRepository).save(violation);
    }

    @Test
    @DisplayName("update modifies value only")
    void update_valueOnly_updatesValue() {
        ThresholdViolationUpdateDTO dto =
                new ThresholdViolationUpdateDTO(
                        null, 42.5F, null, null, null, null, null, null);

        when(thresholdViolationRepository.findById(1L)).thenReturn(Optional.of(violation));
        when(thresholdViolationRepository.save(violation)).thenReturn(violation);

        ThresholdViolation result = thresholdViolationService.update(1L, dto);

        assertThat(result.getValue()).isEqualTo(42.5F);
    }

    @Test
    @DisplayName("update modifies status only")
    void update_violationStatusOnly_updatesStatus() {
        ThresholdViolationUpdateDTO dto =
                new ThresholdViolationUpdateDTO(
                        null, null, ViolationStatus.DISABLED,
                        null, null, null, null, null);

        when(thresholdViolationRepository.findById(1L)).thenReturn(Optional.of(violation));
        when(thresholdViolationRepository.save(violation)).thenReturn(violation);

        ThresholdViolation result = thresholdViolationService.update(1L, dto);

        assertThat(result.getViolationStatus()).isEqualTo(ViolationStatus.DISABLED);
    }

    @Test
    @DisplayName("update sets threshold when provided")
    void update_thresholdId_fetchesAndSetsThreshold() {
        Threshold threshold = new Threshold();

        ThresholdViolationUpdateDTO dto =
                new ThresholdViolationUpdateDTO(
                        null, null, null, null, null, 7L, null, null);

        when(thresholdViolationRepository.findById(1L)).thenReturn(Optional.of(violation));
        when(thresholdService.getThresholdById(7L)).thenReturn(threshold);
        when(thresholdViolationRepository.save(violation)).thenReturn(violation);

        thresholdViolationService.update(1L, dto);

        verify(thresholdService).getThresholdById(7L);
    }

    @Test
    @DisplayName("update sets room when provided")
    void update_roomId_fetchesAndSetsRoom() {
        Room room = new Room();

        ThresholdViolationUpdateDTO dto =
                new ThresholdViolationUpdateDTO(
                        null, null, null, null, null, null, 3L, null);

        when(thresholdViolationRepository.findById(1L)).thenReturn(Optional.of(violation));
        when(roomService.getById(3L)).thenReturn(room);
        when(thresholdViolationRepository.save(violation)).thenReturn(violation);

        thresholdViolationService.update(1L, dto);

        verify(roomService).getById(3L);
    }

    @Test
    @DisplayName("update to resolved sends ISO end timestamp to Raspberry Pi")
    void updateToResolved_sendsIsoEndTimestampToRaspberryPi() {
        String endTimeString = "2026-05-18T15:45:30.123";
        LocalDateTime endTime = LocalDateTime.parse(endTimeString);

        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 10L);

        RaspberryPi raspberryPi = new RaspberryPi();
        ReflectionTestUtils.setField(raspberryPi, "id", 5L);
        room.setRaspberryPi(raspberryPi);

        violation.setRoom(room);
        violation.setEndTime(endTime);
        violation.setViolationStatus(ViolationStatus.ACTIVE);

        ThresholdViolationUpdateDTO dto =
                new ThresholdViolationUpdateDTO(
                        null, null, ViolationStatus.RESOLVED,
                        null, endTime, null, null, null);

        when(thresholdViolationRepository.findById(1L)).thenReturn(Optional.of(violation));
        when(thresholdViolationRepository.save(violation)).thenReturn(violation);
        when(raspberryPiServerService.resolveActiveViolation(eq(5L), any(ViolationResolvedDTO.class)))
                .thenReturn(PiRequestResult.SUCCESS);

        thresholdViolationService.update(1L, dto);

        ArgumentCaptor<ViolationResolvedDTO> captor =
                ArgumentCaptor.forClass(ViolationResolvedDTO.class);
        verify(raspberryPiServerService).resolveActiveViolation(eq(5L), captor.capture());

        assertThat(captor.getValue().endTime())
                .isEqualTo(endTimeString);
    }

    @Test
    @DisplayName("delete removes violation by id")
    void delete_calls_repository() {
        thresholdViolationService.delete(1L);

        verify(thresholdViolationRepository).deleteById(1L);
    }
}
