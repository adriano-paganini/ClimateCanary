package at.qe.skeleton.services;

import at.qe.skeleton.dtos.RoomUpdateDTO;
import at.qe.skeleton.exceptions.RoomNotFoundException;
import at.qe.skeleton.model.Building;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.EmployeeProfile;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.repositories.EmployeeProfileRepository;
import at.qe.skeleton.repositories.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final DepartmentService departmentService;
    private final BuildingService buildingService;
    private final EmployeeProfileRepository employeeProfileRepository;

    public RoomService(RoomRepository repo, DepartmentService departmentService, BuildingService buildingService, EmployeeProfileRepository employeeProfileRepository) {
        this.roomRepository = repo;
        this.departmentService = departmentService;
        this.buildingService = buildingService;
        this.employeeProfileRepository = employeeProfileRepository;
    }

    public List<Room> getAll() {
        return roomRepository.findAllByActiveTrue();
    }

    public Room getById(Long id) {
        return roomRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new RoomNotFoundException("Room with id " + id + " not found"));
    }

    public Room create(Room room) {
        return roomRepository.save(room);
    }

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

    /**
     * Soft delete policy
     * @param id
     */
    @Transactional
    public void delete(Long id) {
        Room room = getById(id);

        // Remove from department's room list
        Department dept = room.getDepartment();
        if (dept != null) {
            dept.getRooms().remove(room);
            room.setDepartment(null);
        }

        // Remove from building's room list
        Building building = room.getBuilding();
        if (building != null) {
            building.getRooms().remove(room);
            room.setBuilding(null);
        }

        // Nullify roomId on any employee profiles pointing to this room
        for (EmployeeProfile ep : room.getEmployeeProfiles()) {
            ep.setRoom(null);
            employeeProfileRepository.save(ep);
        }
        room.getEmployeeProfiles().clear();

        room.setActive(false);
        roomRepository.save(room);
    }
}
