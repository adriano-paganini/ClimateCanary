package at.qe.skeleton.repositories;

import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.models.Metric;
import at.qe.skeleton.models.ThresholdViolation;
import at.qe.skeleton.models.ViolationStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ThresholdViolationRepository extends AbstractRepository<ThresholdViolation, Long> {
    void deleteById(Long id);

    interface DepartmentWarningCount {
        Long getDepartmentId();
        String getDepartmentName();
        Metric getMetric();
        Long getWarningCount();
    }

    @Query("""
    SELECT tv
    FROM ThresholdViolation tv
    JOIN tv.room r
    WHERE (:violationStatus IS NULL OR tv.violationStatus = :violationStatus)
      AND (:roomId IS NULL OR r.id = :roomId)
      AND (:departmentId IS NULL OR r.department.id = :departmentId)
    """)
    List<ThresholdViolation> search(
            @Param("violationStatus") ViolationStatus violationStatus,
            @Param("roomId") Long roomId,
            @Param("departmentId") Long departmentId
    );

    Optional<ThresholdViolation> findByRoomIdAndMetricAndViolationStatus(Long roomId, Metric metric,ViolationStatus violationStatus);

    List<ThresholdViolation> findByThreshold_Id(Long thresholdId);

    List<ThresholdViolation> findByThreshold_IdAndViolationStatus(Long thresholdId, ViolationStatus violationStatus);

    @Query("""
    SELECT r.department.id AS departmentId,
           r.department.name AS departmentName,
           tv.metric AS metric,
           COUNT(tv) AS warningCount
    FROM ThresholdViolation tv
    JOIN tv.room r
    WHERE tv.violationStatus = :violationStatus
      AND r.active = true
      AND r.department IS NOT NULL
    GROUP BY r.department.id, r.department.name, tv.metric
    """)
    List<DepartmentWarningCount> countWarningsByDepartmentAndMetric(
            @Param("violationStatus") ViolationStatus violationStatus
    );
}
