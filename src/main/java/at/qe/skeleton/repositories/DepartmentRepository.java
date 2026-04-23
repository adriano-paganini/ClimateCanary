package at.qe.skeleton.repositories;

import at.qe.skeleton.models.Department;
import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.models.Userx;

import java.util.List;

public interface DepartmentRepository extends AbstractRepository<Department, Long> {

    void deleteById(Long id);

    List<Department> findByDepartmentLeader(Userx departmentLeader);
}
