package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.DepartmentCreateDTO;
import at.qe.skeleton.exceptions.UserNotFoundException;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.model.Userx;
import at.qe.skeleton.repositories.RoomRepository;
import at.qe.skeleton.services.UserxService;
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
