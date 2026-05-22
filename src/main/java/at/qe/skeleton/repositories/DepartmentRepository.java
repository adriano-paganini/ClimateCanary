package at.qe.skeleton.repositories;

import at.qe.skeleton.models.Department;
import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.models.Userx;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DepartmentRepository extends AbstractRepository<Department, Long> {

    void deleteById(Long id);

    List<Department> findByDepartmentLeader(Userx departmentLeader);

    @Modifying
    @Query("UPDATE Department d SET d.departmentLeader = null WHERE d.departmentLeader.id = :userId")
    int clearDepartmentLeaderByUserId(@Param("userId") Long userId);
}
