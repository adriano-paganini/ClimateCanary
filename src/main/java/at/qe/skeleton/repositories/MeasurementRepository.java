package at.qe.skeleton.repositories;

import at.qe.skeleton.models.Metric;
import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.models.Measurement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MeasurementRepository extends AbstractRepository<Measurement, Long> {

    List<Measurement> findByRoomId(Long roomId);
    List<Measurement> findByRoomIdAndMetric(Long roomId, Metric metric);
    List<Measurement> findByTimestampBetween(LocalDateTime from, LocalDateTime to);
    List<Measurement> findByRoomIdAndTimestampBetween(Long roomId, LocalDateTime from, LocalDateTime to);
    List<Measurement> findByRoomIdAndMetricAndTimestampBetween(
            Long roomId,
            Metric metric,
            LocalDateTime from,
            LocalDateTime to
    );

    Optional<Measurement> findTopByRoomIdAndMetricOrderByTimestampDesc(Long roomId, Metric metric);
}