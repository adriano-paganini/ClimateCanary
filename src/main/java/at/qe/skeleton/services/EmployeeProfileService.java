package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.EmployeeProfileNotFoundException;
import at.qe.skeleton.common.exceptions.UserAlreadyExists;
import at.qe.skeleton.dtos.EmployeeProfileUpdateDTO;
import at.qe.skeleton.models.EmployeeProfile;
import at.qe.skeleton.repositories.EmployeeProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeProfileService {

    private final EmployeeProfileRepository employeeProfileRepository;
    private final UserxService userxService;
    private final DepartmentService departmentService;
    private final RoomService roomService;

    public EmployeeProfileService(EmployeeProfileRepository employeeProfileRepository,
                                  UserxService userxService,
                                  DepartmentService departmentService,
                                  RoomService roomService) {
        this.employeeProfileRepository = employeeProfileRepository;
        this.userxService = userxService;
        this.departmentService = departmentService;
        this.roomService = roomService;
    }

    public List<EmployeeProfile> getAll() {
        return employeeProfileRepository.findAll();
    }

    public EmployeeProfile getById(Long id) {
        return employeeProfileRepository.findById(id)
                .orElseThrow(() -> new EmployeeProfileNotFoundException("Employee profile with id " + id + " not found"));
    }

    public EmployeeProfile create(EmployeeProfile employeeProfile) {
        Optional<EmployeeProfile> profile = employeeProfileRepository.findByUser(employeeProfile.getUser());
        EmployeeProfile p = profile.orElse(null);

        if (p != null) {
            throw new UserAlreadyExists("User with id " + employeeProfile.getUser().getId() + " already has a profile");
        }

        return employeeProfileRepository.save(employeeProfile);
    }

    public EmployeeProfile update(Long id, EmployeeProfileUpdateDTO dto) {
        EmployeeProfile existing = getById(id);

        if (dto.userxId() != null) {
            existing.setUser(userxService.getUserById(dto.userxId()));
        }

        if (dto.departmentId() != null) {
            existing.setDepartment(departmentService.getDepartmentById(dto.departmentId()));
        }

        if (dto.roomId() != null) {
            existing.setRoom(roomService.getById(dto.roomId()));
        }

        return employeeProfileRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        EmployeeProfile ep = getById(id);

        if (ep.getUser() != null) {
            ep.getUser().setEmployeeProfile(null);
            ep.setUser(null);
        }

        employeeProfileRepository.deleteById(id);
    }
}
