package at.qe.skeleton.services;

import at.qe.skeleton.exceptions.DepartmentNotFoundException;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.Room;
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

    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException("Department with id " + id + " not found"));
    }

    public Department create(Department d) {
        return departmentRepository.save(d);
    }

    public Department update(Long id, Department updates) {
        Department existing = getDepartmentById(id);

        if (updates.getName() != null) {
            existing.setName(updates.getName());
        }

        if (updates.getRooms() != null) {

            existing.getRooms().forEach(r -> r.setDepartment(null));
            existing.getRooms().clear();

            for (Room room : updates.getRooms()) {
                room.setDepartment(existing);
                existing.getRooms().add(room);
            }
        }

        return departmentRepository.save(existing);
    }

    public void delete(Long id) {
        Department department = getDepartmentById(id);

        for (Room room : department.getRooms()) {
            room.setDepartment(null);
        }

        department.getRooms().clear();
        departmentRepository.deleteById(id);
    }
}
