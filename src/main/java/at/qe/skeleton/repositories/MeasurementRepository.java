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
    List<Measurement> findByRoomIdAndMetricAndTimestampBetween(
            Long roomId,
            Metric metric,
            LocalDateTime from,
            LocalDateTime to
    );

    Optional<Measurement> findTopByRoomIdAndMetricOrderByTimestampDesc(Long roomId, Metric metric);

    interface DepartmentMetricPeriodAverage {
        Long getDepartmentId();
        String getDepartmentName();
        Metric getMetric();
        Double getCurrentAverage();
        Double getPreviousAverage();
    }

    @Query("""
    SELECT r.department.id AS departmentId,
           r.department.name AS departmentName,
           m.metric AS metric,
           AVG(CASE WHEN m.timestamp >= :currentFrom AND m.timestamp <= :to THEN m.measurement ELSE NULL END) AS currentAverage,
           AVG(CASE WHEN m.timestamp >= :previousFrom AND m.timestamp < :currentFrom THEN m.measurement ELSE NULL END) AS previousAverage
    FROM Measurement m
    JOIN m.room r
    WHERE r.active = true
      AND r.department IS NOT NULL
      AND m.timestamp >= :previousFrom
      AND m.timestamp <= :to
    GROUP BY r.department.id, r.department.name, m.metric
    """)
    List<DepartmentMetricPeriodAverage> findDepartmentMetricPeriodAverages(
            @Param("previousFrom") LocalDateTime previousFrom,
            @Param("currentFrom") LocalDateTime currentFrom,
            @Param("to") LocalDateTime to
    );
}
