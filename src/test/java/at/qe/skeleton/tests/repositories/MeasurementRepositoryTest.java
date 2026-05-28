package at.qe.skeleton.tests.repositories;

import at.qe.skeleton.models.Measurement;
import at.qe.skeleton.models.Metric;
import at.qe.skeleton.repositories.MeasurementRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MeasurementRepositoryTest {

    @Autowired
    private MeasurementRepository measurementRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long roomIdByName(String roomName) {
        return jdbcTemplate.queryForObject(
                "SELECT ID FROM ROOMS WHERE NAME = ?",
                Long.class,
                roomName
        );
    }

    @Test
    void findByRoomId_shouldReturnAllMeasurementsForRoom1() {
        Long roomId = roomIdByName("Room 1");

        List<Measurement> measurements = measurementRepository.findByRoomId(roomId);

        assertThat(measurements).hasSizeGreaterThanOrEqualTo(34_563);
        assertThat(measurements)
                .extracting(Measurement::getMetric)
                .contains(
                        Metric.HUMIDITY,
                        Metric.TEMPERATURE,
                        Metric.PRESSURE,
                        Metric.IAQ
                );
    }

    @Test
    void findByRoomId_shouldReturnOnlyMeasurementsForThatRoom() {
        Long roomId = roomIdByName("Common Area 1");

        List<Measurement> measurements = measurementRepository.findByRoomId(roomId);

        assertThat(measurements).hasSize(1);
        assertThat(measurements.getFirst().getMetric()).isEqualTo(Metric.TEMPERATURE);
        assertThat(measurements.getFirst().getMeasurement()).isEqualTo(21f);
    }

    @Test
    void findByRoomIdAndMetric_shouldReturnMatchingMeasurement() {
        Long roomId = roomIdByName("Room 1");

        List<Measurement> measurements =
                measurementRepository.findByRoomIdAndMetric(roomId, Metric.HUMIDITY);

        assertThat(measurements).hasSizeGreaterThanOrEqualTo(8_641);

        assertThat(measurements)
                .allMatch(measurement -> measurement.getMetric() == Metric.HUMIDITY)
                .allMatch(measurement -> measurement.getRoom().getId().equals(roomId));
        assertThat(measurements)
                .anyMatch(measurement -> measurement.getMeasurement() == 65f);
    }

    @Test
    void findByRoomIdAndMetric_shouldReturnEmptyListWhenNoMeasurementExists() {
        Long roomId = roomIdByName("Common Area 1");

        List<Measurement> measurements =
                measurementRepository.findByRoomIdAndMetric(roomId, Metric.HUMIDITY);

        assertThat(measurements).isEmpty();
    }

    @Test
    void findByTimestampBetween_shouldReturnMeasurementsInsideTimeframe() {
        LocalDateTime from = LocalDateTime.of(2026, 4, 23, 7, 0);
        LocalDateTime to = LocalDateTime.of(2026, 4, 23, 9, 0);

        List<Measurement> measurements =
                measurementRepository.findByTimestampBetween(from, to);

        assertThat(measurements).hasSize(4);
    }

    @Test
    void findByTimestampBetween_shouldReturnEmptyListOutsideTimeframe() {
        LocalDateTime from = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 2, 0, 0);

        List<Measurement> measurements =
                measurementRepository.findByTimestampBetween(from, to);

        assertThat(measurements).isEmpty();
    }

    @Test
    void findByRoomIdAndTimestampBetween_shouldReturnOnlyMeasurementsForRoomInTimeframe() {
        Long roomId = roomIdByName("Room 1");

        LocalDateTime from = LocalDateTime.of(2026, 4, 23, 7, 0);
        LocalDateTime to = LocalDateTime.of(2026, 4, 23, 9, 0);

        List<Measurement> measurements =
                measurementRepository.findByRoomIdAndTimestampBetween(roomId, from, to);


        assertThat(measurements)
                .allMatch(measurement -> measurement.getRoom().getId().equals(roomId))
                .hasSize(3);
    }

    @Test
    void findByRoomIdAndMetricAndTimestampBetween_shouldReturnMatchingMeasurement() {
        Long roomId = roomIdByName("Room 1");

        LocalDateTime from = LocalDateTime.of(2026, 4, 23, 7, 0);
        LocalDateTime to = LocalDateTime.of(2026, 4, 23, 9, 0);

        List<Measurement> measurements =
                measurementRepository.findByRoomIdAndMetricAndTimestampBetween(
                        roomId,
                        Metric.TEMPERATURE,
                        from,
                        to
                );

        assertThat(measurements).hasSize(1);

        Measurement measurement = measurements.getFirst();
        assertThat(measurement.getMetric()).isEqualTo(Metric.TEMPERATURE);
        assertThat(measurement.getMeasurement()).isEqualTo(22f);
    }

}
