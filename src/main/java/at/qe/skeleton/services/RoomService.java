package at.qe.skeleton.services;

import at.qe.skeleton.dtos.RoomUpdateDTO;
import at.qe.skeleton.exceptions.RoomNotFoundException;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.repositories.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final DepartmentService departmentService;
    private final BuildingService buildingService;

    public RoomService(RoomRepository repo, DepartmentService departmentService, BuildingService buildingService) {
        this.roomRepository = repo;
        this.departmentService = departmentService;
        this.buildingService = buildingService;
    }

    public List<Room> getAll() {
        return roomRepository.findAll();
    }

    public Room getById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException("Room with id " + id + " not found"));
    }

    public Room create(Room room) {
        return roomRepository.save(room);
    }

    /* Do not remove null checks, some fields may be null as they are optional despite what Intellij is suggesting */
    public Room update(Long id, RoomUpdateDTO dto) {
        Room existing = getById(id);

        if (dto.name() != null) {
            existing.setName(dto.name());
        }

        if (dto.roomType() != null) {
            existing.setRoomType(dto.roomType());
        }

        if (dto.minOccupancy() != null) {
            existing.setMinOccupancy(dto.minOccupancy());
        }

        if (dto.departmentId() != null) {
            existing.setDepartment(departmentService.getDepartmentById(dto.departmentId()));
        }

        if (dto.buildingId() != null) {
            existing.setBuilding(buildingService.getBuildingById(dto.buildingId()));
        }

        return roomRepository.save(existing);
    }

    public void delete(Long id) {
        roomRepository.deleteById(id);
    }
}
