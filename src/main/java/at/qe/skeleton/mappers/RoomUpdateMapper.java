package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.RoomUpdateDTO;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.services.DepartmentService;
import org.springframework.stereotype.Service;

@Service
public class RoomUpdateMapper implements DTOMapper<Room, RoomUpdateDTO> {

    private final DepartmentService departmentService;

    public RoomUpdateMapper(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @Override
    public Room mapFrom(RoomUpdateDTO dto) {

        Room room = new Room();

        room.setName(dto.name());
        room.setRoomType(dto.roomType());
        room.setMinOccupancy(dto.minOccupancy());


        if (dto.departmentId() != null) {
            Department department = departmentService.getDepartmentById(dto.departmentId());
            room.setDepartment(department);
        }

        return room;
    }

    @Override
    public RoomUpdateDTO mapTo(Room entity) {
        throw new UnsupportedOperationException();
    }
}
