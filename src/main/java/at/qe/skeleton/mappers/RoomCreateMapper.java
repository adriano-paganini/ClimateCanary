package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.RoomCreateDTO;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.Room;
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

        Department department = departmentService.getById(dto.departmentId());
        room.setDepartment(department);

        Building building = buildingService.getBuildingById(dto.buildingId());
        room.setBuilding(building);

        return room;
    }

    @Override
    public RoomCreateDTO mapTo(Room entity) {
        throw new UnsupportedOperationException();
    }
}
