package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.ConflictException;
import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.EmployeeProfileUpdateDTO;
import at.qe.skeleton.models.EmployeeProfile;
import at.qe.skeleton.repositories.EmployeeProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
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
                .orElseThrow(() -> new NotFoundException("Employee profile with id " + id + " not found"));
    }

    public EmployeeProfile create(EmployeeProfile employeeProfile) {
        Optional<EmployeeProfile> profile = employeeProfileRepository.findByUser(employeeProfile.getUser());
        EmployeeProfile p = profile.orElse(null);

        if (p != null) {
            throw new ConflictException("User with id " + employeeProfile.getUser().getId() + " already has a profile");
        }

        EmployeeProfile savedProfile = employeeProfileRepository.save(employeeProfile);

        log.info("Created employee profile with id={} for userId={}",
                savedProfile.getId(),
                savedProfile.getUser() != null ? savedProfile.getUser().getId() : null);
        log.debug("Created employee profile details: id={}, userId={}, departmentId={}, roomId={}",
                savedProfile.getId(),
                savedProfile.getUser() != null ? savedProfile.getUser().getId() : null,
                savedProfile.getDepartment() != null ? savedProfile.getDepartment().getId() : null,
                savedProfile.getRoom() != null ? savedProfile.getRoom().getId() : null);

        return savedProfile;
    }

    public EmployeeProfile update(Long id, EmployeeProfileUpdateDTO dto) {
        EmployeeProfile existing = getById(id);

        StringBuilder debugInfo = new StringBuilder("Updated employee profile details:")
                .append(" id=").append(id);

        if (dto.userxId() != null) {
            existing.setUser(userxService.getUserById(dto.userxId()));
            debugInfo.append(", userxId=").append(dto.userxId());
        }

        if (dto.departmentId() != null) {
            existing.setDepartment(departmentService.getDepartmentById(dto.departmentId()));
            debugInfo.append(", departmentId=").append(dto.departmentId());
        }

        if (dto.roomId() != null) {
            existing.setRoom(roomService.getById(dto.roomId()));
            debugInfo.append(", roomId=").append(dto.roomId());
        }

        EmployeeProfile updatedProfile = employeeProfileRepository.save(existing);

        log.info("Updated employee profile with id={}", id);
        log.debug(debugInfo.toString());

        return updatedProfile;
    }

    @Transactional
    public void delete(Long id) {
        EmployeeProfile ep = getById(id);

        if (ep.getUser() != null) {
            ep.getUser().setEmployeeProfile(null);
            ep.setUser(null);
        }

        employeeProfileRepository.deleteById(id);

        log.info("Deleted employee profile with id={}", id);
        log.debug("Removed user association before deleting employee profile id={}", id);
    }
}