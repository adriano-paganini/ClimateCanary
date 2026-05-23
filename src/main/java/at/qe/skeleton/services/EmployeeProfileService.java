package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.ConflictException;
import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.EmployeeProfileUpdateDTO;
import at.qe.skeleton.models.EmployeeProfile;
import at.qe.skeleton.models.Userx;
import at.qe.skeleton.models.UserxRole;
import at.qe.skeleton.repositories.EmployeeProfileRepository;
import at.qe.skeleton.repositories.UserxRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class EmployeeProfileService {

    private final EmployeeProfileRepository employeeProfileRepository;
    private final UserxRepository userxRepository;
    private final DepartmentService departmentService;
    private final RoomService roomService;
    private final AuthenticatedUserService authentication;

    public EmployeeProfileService(EmployeeProfileRepository employeeProfileRepository,
                                  UserxRepository userxRepository,
                                  DepartmentService departmentService,
                                  RoomService roomService,
                                  AuthenticatedUserService authentication) {
        this.employeeProfileRepository = employeeProfileRepository;
        this.userxRepository = userxRepository;
        this.departmentService = departmentService;
        this.roomService = roomService;
        this.authentication = authentication;
    }

    public List<EmployeeProfile> getAll(Long userId, Long departmentId) {

        if (userId != null && departmentId != null) {
            return employeeProfileRepository.findByUser_IdAndDepartment_Id(userId, departmentId);
        }

        if (userId != null) return employeeProfileRepository.findByUser_Id(userId);
        if (departmentId != null) return employeeProfileRepository.findByDepartment_Id(departmentId);

        return employeeProfileRepository.findAll();
    }

    public Optional<EmployeeProfile> getMyProfile() {
        Long id = authentication.getAuthenticatedUser().getId();
        return employeeProfileRepository.getByUser_Id(id);
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
            existing.setUser(userxRepository.findById(dto.userxId())
                    .orElseThrow(() -> new NotFoundException("User with id " + dto.userxId() + " not found")));
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
            Userx user = ep.getUser();
            ep.getUser().setEmployeeProfile(null);

            Set<UserxRole> roles = user.getRoles();
            if (roles != null) {
                roles.remove(UserxRole.EMPLOYEE);
                user.setRoles(roles);
            }

            user.setEmployeeProfile(null);
            ep.setUser(null);

            userxRepository.save(user);
            log.info("Updated user roles after deleting employee profile with id={}", id);
        }

        employeeProfileRepository.deleteById(id);

        log.info("Deleted employee profile with id={}", id);
    }
}
