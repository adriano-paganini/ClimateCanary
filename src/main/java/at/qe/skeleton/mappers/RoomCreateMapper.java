package at.qe.skeleton.mappers;

import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.dtos.RoomCreateDTO;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.services.BuildingService;
import at.qe.skeleton.services.DepartmentService;
import org.springframework.stereotype.Service;

@Service
public class RoomCreateMapper implements DTOMapper<Room, RoomCreateDTO> {

    private final DepartmentService departmentService;
    private final BuildingService buildingService;

    public  RoomCreateMapper(DepartmentService departmentService, BuildingService buildingService) {
        this.departmentService = departmentService;
        this.buildingService = buildingService;
    }

    @Override
    public Room mapFrom(RoomCreateDTO dto) {
        Room room = new Room();
        room.setName(dto.name());
        room.setRoomType(dto.roomType());
        room.setMinOccupancy(dto.minOccupancy());
        room.setDepartment(departmentService.getDepartmentById(dto.departmentId()));
        room.setBuilding(buildingService.getBuildingById(dto.buildingId()));
        room.setActive(true);
        return room;
    }

    @Override
    public RoomCreateDTO mapTo(Room entity) {
        throw new UnsupportedOperationException();
    }
}
