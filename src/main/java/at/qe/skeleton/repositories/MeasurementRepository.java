package at.qe.skeleton.repositories;

import at.qe.skeleton.models.Metric;
import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.models.Measurement;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MeasurementRepository extends AbstractRepository<Measurement, Long> {

    List<Measurement> findByRoomId(Long roomId);
    List<Measurement> findByRoomIdAndMetric(Long roomId, Metric metric);
    List<Measurement> findByTimestampBetween(LocalDateTime from, LocalDateTime to);
    List<Measurement> findByRoomIdAndTimestampBetween(Long roomId, LocalDateTime from, LocalDateTime to);
    List<Measurement> findByRoomIdAndMetricAndTimestampBetween(Long roomId, Metric metric,
                                                               LocalDateTime from,
                                                               LocalDateTime to);

    @Query("""
            SELECT m FROM Measurement m
            WHERE m.room.id = :roomId
              AND m.metric = :metric
            ORDER BY m.timestamp DESC
            LIMIT 1
            """)
    Optional<Measurement> findLatestByRoomIdAndMetric(
            @Param("roomId") Long roomId,
            @Param("metric") Metric metric);

}
