package at.qe.skeleton.department.repository;

import at.qe.skeleton.department.model.Department;
import at.qe.skeleton.common.AbstractRepository;

public interface DepartmentRepository extends AbstractRepository<Department, Long> {

    void deleteById(Long id);
}
