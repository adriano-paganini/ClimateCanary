package at.qe.skeleton.department.service;

import at.qe.skeleton.department.dto.DepartmentUpdateDTO;
import at.qe.skeleton.common.exceptions.DepartmentNotFoundException;
import at.qe.skeleton.common.exceptions.UserNotFoundException;
import at.qe.skeleton.department.model.Department;
import at.qe.skeleton.employeeprofile.model.EmployeeProfile;
import at.qe.skeleton.room.model.Room;
import at.qe.skeleton.userx.model.Userx;
import at.qe.skeleton.department.repository.DepartmentRepository;
import at.qe.skeleton.employeeprofile.repository.EmployeeProfileRepository;
import at.qe.skeleton.room.repository.RoomRepository;
import at.qe.skeleton.userx.service.UserxService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final RoomRepository roomRepository;
    private final UserxService userxService;
    private final EmployeeProfileRepository employeeProfileRepository;

    public DepartmentService(DepartmentRepository repo, RoomRepository roomRepository, UserxService userxService, EmployeeProfileRepository employeeProfileRepository) {
        this.departmentRepository = repo;
        this.roomRepository = roomRepository;
        this.userxService = userxService;
        this.employeeProfileRepository = employeeProfileRepository;
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
            roomRepository.save(room);
        }
        department.getRooms().clear();

        for (EmployeeProfile ep : department.getEmployeeProfiles()) {
            ep.setDepartment(null);
            employeeProfileRepository.save(ep);
        }
        department.getEmployeeProfiles().clear();

        departmentRepository.deleteById(id);
    }
}
