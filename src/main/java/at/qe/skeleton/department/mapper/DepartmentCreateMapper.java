package at.qe.skeleton.department.mapper;

import at.qe.skeleton.department.dto.DepartmentCreateDTO;
import at.qe.skeleton.common.exceptions.UserNotFoundException;
import at.qe.skeleton.department.model.Department;
import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.room.model.Room;
import at.qe.skeleton.userx.model.Userx;
import at.qe.skeleton.room.repository.RoomRepository;
import at.qe.skeleton.userx.service.UserxService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentCreateMapper implements DTOMapper<Department, DepartmentCreateDTO> {

    private final RoomRepository roomRepository;
    private final UserxService userxService;

    public  DepartmentCreateMapper(RoomRepository roomRepository, UserxService userxService) {
        this.roomRepository = roomRepository;
        this.userxService = userxService;
    }

    @Override
    public DepartmentCreateDTO mapTo(Department entity) {
        throw new  UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Department mapFrom(DepartmentCreateDTO dto) {
        Department department = new Department();
        Userx user = userxService.loadUser(dto.departmentLeadId())
                .orElseThrow(() -> new UserNotFoundException("User with id " + dto.departmentLeadId() + " not found."));

        department.setName(dto.name());
        department.setDepartmentLeader(user);

        if (dto.roomIds() != null) {
            List<Room> rooms = roomRepository.findAllByIdsAndActiveTrue(dto.roomIds());

            department.setRooms(rooms);
            rooms.forEach(room -> room.setDepartment(department));
        }

        return department;
    }
}
