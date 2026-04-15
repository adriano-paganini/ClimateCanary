package at.qe.skeleton.employeeprofile.mapper;

import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.employeeprofile.dto.EmployeeProfileCreateDTO;
import at.qe.skeleton.employeeprofile.model.EmployeeProfile;
import at.qe.skeleton.department.service.DepartmentService;
import at.qe.skeleton.room.service.RoomService;
import at.qe.skeleton.userx.service.UserxService;
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
