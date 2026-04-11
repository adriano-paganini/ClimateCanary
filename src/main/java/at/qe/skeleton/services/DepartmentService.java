package at.qe.skeleton.services;

import at.qe.skeleton.exceptions.DepartmentNotFoundException;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.repositories.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository repo) {
        this.departmentRepository = repo;
    }

    public List<Department> getAll() {
        return departmentRepository.findAll();
    }

    public Department getById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException("Department with id " + id + " not found"));
    }

    public Department create(Department d) {
        return departmentRepository.save(d);
    }

    public Department update(Long id, Department updated) {
        Department d = getById(id);
        d.setName(updated.getName());
        return departmentRepository.save(d);
    }

    public void delete(Long id) {
        departmentRepository.deleteById(id);
    }
}
