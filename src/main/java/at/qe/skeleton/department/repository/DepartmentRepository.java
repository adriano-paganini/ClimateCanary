package at.qe.skeleton.department.repository;

import at.qe.skeleton.department.model.Department;
import at.qe.skeleton.common.AbstractRepository;
import at.qe.skeleton.userx.model.Userx;

import java.util.List;

public interface DepartmentRepository extends AbstractRepository<Department, Long> {

    void deleteById(Long id);

    List<Department> findByDepartmentLeader(Userx departmentLeader);
}
