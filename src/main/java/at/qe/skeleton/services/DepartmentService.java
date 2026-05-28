package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.ConflictException;
import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.DepartmentUpdateDTO;
import at.qe.skeleton.models.*;
import at.qe.skeleton.repositories.DepartmentRepository;
import at.qe.skeleton.repositories.EmployeeProfileRepository;
import at.qe.skeleton.repositories.RoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Slf4j
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

    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN', 'DEPARTMENT_LEAD', 'MANAGEMENT', 'EMPLOYEE')")
    public List<Department> getAll() {
        return departmentRepository.findAll();
    }

    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'BUILDING_ADMIN', 'DEPARTMENT_LEAD', 'MANAGEMENT', 'EMPLOYEE')")
    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department with id " + id + " not found"));
    }

    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public Department create(Department d) {
        Department savedDepartment = departmentRepository.save(d);

        log.info("Created department with id={}", savedDepartment.getId());
        log.debug("Created department details: id={}, name={}, leaderId={}",
                savedDepartment.getId(),
                savedDepartment.getName(),
                savedDepartment.getDepartmentLeader() != null ? savedDepartment.getDepartmentLeader().getId() : null);
        ensureDepartmentLeadRole(savedDepartment.getDepartmentLeader());
        return savedDepartment;
    }

    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public Department update(Long id, DepartmentUpdateDTO dto) {
        Department existing = getDepartmentById(id);

        StringBuilder debugInfo = new StringBuilder("Updated department details:")
                .append(" id=").append(id);

        if (dto.name() != null) {
            existing.setName(dto.name());
            debugInfo.append(", name=").append(dto.name());
        }

        if (dto.roomIds() != null) {
            existing.getRooms().forEach(r -> r.setDepartment(null));
            existing.getRooms().clear();

            List<Room> rooms = roomRepository.findAllByIdsAndActiveTrue(dto.roomIds());
            rooms.forEach(r -> r.setDepartment(existing));
            existing.setRooms(rooms);

            debugInfo.append(", roomIds=").append(dto.roomIds());
        }

        if (dto.departmentLeadId() != null) {
            Userx previousLeader = existing.getDepartmentLeader();
            Userx leader = userxService.loadUser(dto.departmentLeadId())
                    .orElseThrow(() -> new NotFoundException("User with id " + dto.departmentLeadId() + " not found"));
            existing.setDepartmentLeader(leader);
            ensureDepartmentLeadRole(leader);
            removeDepartmentLeadRoleIfNoLongerLeading(previousLeader, existing);
            debugInfo.append(", departmentLeadId=").append(dto.departmentLeadId());
        }

        Department updatedDepartment = departmentRepository.save(existing);

        log.info("Updated department with id={}", id);
        log.debug(debugInfo.toString());

        return updatedDepartment;
    }
    private void removeDepartmentLeadRoleIfNoLongerLeadingAnyDepartment(Userx leader) {
        if (leader == null) {
            return;
        }

        if (leader.getRoles() == null || !leader.getRoles().contains(UserxRole.DEPARTMENT_LEAD)) {
            return;
        }

        boolean stillLeadsAnyDepartment = !departmentRepository.findByDepartmentLeader(leader).isEmpty();

        if (!stillLeadsAnyDepartment) {
            leader.getRoles().remove(UserxRole.DEPARTMENT_LEAD);
            userxService.saveUser(leader);

            log.info("Removed department lead role from user with id={} after department deletion", leader.getId());
            log.debug("Removed department lead role from user with id={} and roles={}", leader.getId(), leader.getRoles());
        }
    }

    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public void delete(Long id) {
        Department department = getDepartmentById(id);
        long assignedEmployeeCount = employeeProfileRepository.countByDepartment_Id(id);
        if (assignedEmployeeCount > 0) {
            throw new ConflictException("Department with id " + id + " has " + assignedEmployeeCount + " assigned employees");
        }

        Userx previousLeader = department.getDepartmentLeader();

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

        removeDepartmentLeadRoleIfNoLongerLeadingAnyDepartment(previousLeader);

        log.info("Deleted department with id={}", id);
        log.debug("Cleared room and employee profile associations before deleting department id={}", id);
    }

    private void ensureDepartmentLeadRole(Userx leader) {
        if (leader == null) {
            return;
        }

        if (leader.getRoles() == null) {
            leader.setRoles(new HashSet<>());
        }

        if (!leader.getRoles().contains(UserxRole.DEPARTMENT_LEAD)) {
            leader.getRoles().add(UserxRole.DEPARTMENT_LEAD);
            userxService.saveUser(leader);
            log.info("Added department lead role to user with id={}", leader.getId());
            log.debug("Added department lead role to user with id={} and roles={}", leader.getId(), leader.getRoles());
        }
    }

    private void removeDepartmentLeadRoleIfNoLongerLeading(Userx previousLeader, Department updatedDepartment) {
        if (previousLeader == null || previousLeader.equals(updatedDepartment.getDepartmentLeader())) {
            return;
        }

        if (previousLeader.getRoles() == null || !previousLeader.getRoles().contains(UserxRole.DEPARTMENT_LEAD)) {
            return;
        }

        boolean leadsAnotherDepartment = departmentRepository.findByDepartmentLeader(previousLeader)
                .stream()
                .anyMatch(department -> !department.equals(updatedDepartment));

        if (!leadsAnotherDepartment) {
            previousLeader.getRoles().remove(UserxRole.DEPARTMENT_LEAD);
            userxService.saveUser(previousLeader);
            log.info("Removed department lead role from user with id={}", previousLeader.getId());
            log.debug("Removed department lead role from user with id={} and roles={}", previousLeader.getId(), previousLeader.getRoles());
        }
    }
}
