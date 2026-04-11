package at.qe.skeleton.repositories;

import at.qe.skeleton.model.Department;

public interface DepartmentRepository extends AbstractRepository<Department, Long> {

    void deleteById(Long id);
}
