package at.qe.skeleton.tests.services;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.models.Measurement;
import at.qe.skeleton.models.Metric;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.repositories.MeasurementRepository;
import at.qe.skeleton.repositories.RoomRepository;
import at.qe.skeleton.services.MeasurementService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class MeasurementServiceTest {

    @Mock
    private MeasurementRepository repository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private MeasurementService service;

    private Room room;
    private Measurement tempMeasurement;
    private Measurement humMeasurement;

    @BeforeEach
    void setUp() {
        room = new Room();
        roomRepository.save(room);

        tempMeasurement = createMeasurement(room, Metric.TEMPERATURE);
        humMeasurement = createMeasurement(room, Metric.HUMIDITY);
    }

    private Measurement createMeasurement(Room room, Metric metric) {
        Measurement m = new Measurement();
        m.setRoom(room);
        m.setMetric(metric);
        m.setTimestamp(LocalDateTime.now());
        return m;
    }

    @Test
    @DisplayName("getAll returns all measurements")
    void getAll_returnsAll() {
        List<Measurement> expected = List.of(tempMeasurement);

        Mockito.when(repository.findAll()).thenReturn(expected);

        List<Measurement> result = service.getAll();

        Assertions.assertThat(result).isEqualTo(expected);
        Mockito.verify(repository).findAll();
    }

    @Test
    @DisplayName("getAll returns empty list when none exist")
    void getAll_returnsEmpty() {
        Mockito.when(repository.findAll()).thenReturn(List.of());

        Assertions.assertThat(service.getAll()).isEmpty();
    }

    @Test
    @DisplayName("getById returns measurement when found")
    void getById_returnsMeasurement() {
        Mockito.when(repository.findById(1L)).thenReturn(Optional.of(tempMeasurement));

        Measurement result = service.getById(1L);

        Assertions.assertThat(result).isEqualTo(tempMeasurement);
    }

    @Test
    @DisplayName("getById throws when not found")
    void getById_throwsWhenMissing() {
        Mockito.when(repository.findById(1L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> service.getById(1L))
                .isInstanceOf(NotFoundException.class);
    }

    private final LocalDateTime from = LocalDateTime.now().minusDays(1);
    private final LocalDateTime to = LocalDateTime.now();

    @Test
    @DisplayName("getFiltered with all filters")
    void getFiltered_allFilters() {
        List<Measurement> results = List.of(tempMeasurement);

        Mockito.when(repository.findByRoomIdAndMetricAndTimestampBetween(
                        1L, Metric.TEMPERATURE, from, to))
                .thenReturn(results);

        List<Measurement> result = service.getFiltered(
                1L, Metric.TEMPERATURE, from, to);

        Assertions.assertThat(result).isEqualTo(results);
    }

    @Test
    @DisplayName("getFiltered room + metric only")
    void getFiltered_roomAndMetric() {
        List<Measurement> results = List.of(humMeasurement);

        Mockito.when(repository.findByRoomIdAndMetric(1L, Metric.TEMPERATURE))
                .thenReturn(results);

        List<Measurement> result = service.getFiltered(
                1L, Metric.TEMPERATURE, null, null);

        Assertions.assertThat(result).isEqualTo(results);
    }

    @Test
    @DisplayName("getFiltered room + range only")
    void getFiltered_roomAndRange() {
        List<Measurement> results = List.of(tempMeasurement);

        Mockito.when(repository.findByRoomIdAndTimestampBetween(1L, from, to))
                .thenReturn(results);

        List<Measurement> result = service.getFiltered(
                1L, null, from, to);

        Assertions.assertThat(result).isEqualTo(results);
    }

    @Test
    @DisplayName("getFiltered room only")
    void getFiltered_roomOnly() {
        List<Measurement> results = List.of(tempMeasurement);

        Mockito.when(repository.findByRoomId(1L))
                .thenReturn(results);

        List<Measurement> result = service.getFiltered(
                1L, null, null, null);

        Assertions.assertThat(result).isEqualTo(results);
    }

    @Test
    @DisplayName("getFiltered no filters")
    void getFiltered_noFilters() {
        List<Measurement> results = List.of(tempMeasurement);

        Mockito.when(repository.findAll()).thenReturn(results);

        List<Measurement> result = service.getFiltered(null, null, null, null);

        Assertions.assertThat(result).isEqualTo(results);
    }

    // ─────────────────────────────────────────────
    // getLatestPerMetric
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getLatestPerMetric returns populated map")
    void getLatestPerMetric_populated() {

        Mockito.when(repository.findLatestByRoomIdAndMetric(room.getId(), Metric.TEMPERATURE))
                .thenReturn(Optional.of(tempMeasurement));

        Mockito.when(repository.findLatestByRoomIdAndMetric(room.getId(), Metric.HUMIDITY))
                .thenReturn(Optional.of(humMeasurement));

        for (Metric m : Metric.values()) {
            if (m != Metric.TEMPERATURE && m != Metric.HUMIDITY) {
                Mockito.when(repository.findLatestByRoomIdAndMetric(room.getId(), m))
                        .thenReturn(Optional.empty());
            }
        }

        Map<Metric, Measurement> result = service.getLatestPerMetric(room.getId());

        Assertions.assertThat(result)
                .containsEntry(Metric.TEMPERATURE, tempMeasurement)
                .containsEntry(Metric.HUMIDITY, humMeasurement);
    }

    @Test
    @DisplayName("getLatestPerMetric returns empty map")
    void getLatestPerMetric_empty() {
        for (Metric m : Metric.values()) {
            Mockito.when(repository.findLatestByRoomIdAndMetric(room.getId(), m))
                    .thenReturn(Optional.empty());
        }

        Map<Metric, Measurement> result = service.getLatestPerMetric(room.getId());

        Assertions.assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getLatestPerMetric queries all metrics")
    void getLatestPerMetric_queriesAllMetrics() {
        for (Metric m : Metric.values()) {
            Mockito.when(repository.findLatestByRoomIdAndMetric(room.getId(), m))
                    .thenReturn(Optional.empty());
        }

        service.getLatestPerMetric(room.getId());

        for (Metric m : Metric.values()) {
            Mockito.verify(repository).findLatestByRoomIdAndMetric(room.getId(), m);
        }
    }
}