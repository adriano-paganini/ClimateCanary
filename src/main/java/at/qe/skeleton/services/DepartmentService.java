package at.qe.skeleton.services;

import at.qe.skeleton.dtos.DepartmentUpdateDTO;
import at.qe.skeleton.exceptions.DepartmentNotFoundException;
import at.qe.skeleton.exceptions.UserNotFoundException;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.DepartmentRepository;
import at.qe.skeleton.repositories.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final RoomRepository roomRepository;
    private final UserxService userxService;

    public DepartmentService(DepartmentRepository repo, RoomRepository roomRepository, UserxService userxService) {
        this.departmentRepository = repo;
        this.roomRepository = roomRepository;
        this.userxService = userxService;
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

    public Department update(Long id, DepartmentUpdateDTO dto) {
        Department existing = getDepartmentById(id);

        if (dto.name() != null) {
            existing.setName(dto.name());
        }

        if (dto.roomIds() != null) {
            existing.getRooms().forEach(r -> r.setDepartment(null));
            existing.getRooms().clear();

            List<Room> rooms = roomRepository.findAllByIdsAndActiveTrue(dto.roomIds());
            rooms.forEach(r -> r.setDepartment(existing));
            existing.setRooms(rooms);
        }

        if (dto.departmentLeadId() != null) {
            Userx leader = userxService.loadUser(dto.departmentLeadId())
                    .orElseThrow(() -> new UserNotFoundException("User with id " + dto.departmentLeadId() + " not found"));
            existing.setDepartmentLeader(leader);
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
