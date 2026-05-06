package at.qe.skeleton.repositories;

import at.qe.skeleton.models.Absence;
import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.models.Userx;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AbsenceRepository extends AbstractRepository<Absence, Long> {

    void deleteById(Long id);

    List<Absence> findByUser(Userx user);

    @Query("""
        SELECT a
        FROM Absence a
        JOIN EmployeeProfile e ON e.user.id = a.user.id
        WHERE (:userId IS NULL OR a.user.id = :userId)
          AND (:departmentId IS NULL OR e.department.id = :departmentId)
    """)
    List<Absence> search(@Param("userId") Long userId, @Param("departmentId") Long departmentId);

    @Query("""
        SELECT a
        FROM Absence a
        WHERE a.startDate <= :to
          AND a.endDate >= :from
""")
    List<Absence> findByTimeframe(@Param("from")LocalDateTime from,@Param("to") LocalDateTime to);
}
