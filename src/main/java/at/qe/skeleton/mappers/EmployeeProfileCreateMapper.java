package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.EmployeeProfileCreateDTO;
import at.qe.skeleton.model.EmployeeProfile;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.services.DepartmentService;
import at.qe.skeleton.services.RoomService;
import at.qe.skeleton.services.UserxService;
import org.springframework.stereotype.Service;

@Service
public class EmployeeProfileCreateMapper implements DTOMapper<EmployeeProfile, EmployeeProfileCreateDTO> {

    private final UserxService userxService;
    private final DepartmentService departmentService;
    private final RoomService roomService;

    public EmployeeProfileCreateMapper(UserxService userxService, DepartmentService departmentService, RoomService roomService) {
        this.userxService = userxService;
        this.departmentService = departmentService;
        this.roomService = roomService;
    }

    @Override
    public EmployeeProfileCreateDTO mapTo(EmployeeProfile entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EmployeeProfile mapFrom(EmployeeProfileCreateDTO dto) {
        EmployeeProfile employeeProfile = new EmployeeProfile();
        employeeProfile.setUser(userxService.getUserById(dto.userxId()));
        employeeProfile.setDepartment(departmentService.getDepartmentById(dto.departmentId()));
        employeeProfile.setRoom(roomService.getById(dto.roomId()));
        return employeeProfile;
    }
}
