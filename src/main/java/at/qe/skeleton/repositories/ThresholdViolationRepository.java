package at.qe.skeleton.repositories;

import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.models.ThresholdViolation;
import at.qe.skeleton.models.ViolationStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ThresholdViolationRepository extends AbstractRepository<ThresholdViolation, Long> {
    void deleteById(Long id);

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
}
