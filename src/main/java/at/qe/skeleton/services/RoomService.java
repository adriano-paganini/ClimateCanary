package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.RoomNotFoundException;
import at.qe.skeleton.models.EmployeeProfile;
import at.qe.skeleton.models.DeviceStatus;
import at.qe.skeleton.dtos.RoomUpdateDTO;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.repositories.RoomRepository;
import at.qe.skeleton.models.SensorStation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
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
        return roomRepository.findAllByActiveTrue();
    }

    public Room getById(Long id) {
        return roomRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new RoomNotFoundException("Room with id " + id + " not found"));
    }

    public Room create(Room room) {
        Room savedRoom = roomRepository.save(room);

        log.info("Created room with id={}", savedRoom.getId());
        log.debug("Created room details: id={}, name={}, roomType={}, minOccupancy={}, departmentId={}, buildingId={}",
                savedRoom.getId(),
                savedRoom.getName(),
                savedRoom.getRoomType(),
                savedRoom.getMinOccupancy(),
                savedRoom.getDepartment() != null ? savedRoom.getDepartment().getId() : null,
                savedRoom.getBuilding() != null ? savedRoom.getBuilding().getId() : null);

        return savedRoom;
    }

    public Room update(Long id, RoomUpdateDTO dto) {
        Room existing = getById(id);

        StringBuilder debugInfo = new StringBuilder("Updated room details:")
                .append(" id=").append(id);

        if (dto.name() != null) {
            existing.setName(dto.name());
            debugInfo.append(", name=").append(dto.name());
        }

        if (dto.roomType() != null) {
            existing.setRoomType(dto.roomType());
            debugInfo.append(", roomType=").append(dto.roomType());
        }

        if (dto.minOccupancy() != null) {
            existing.setMinOccupancy(dto.minOccupancy());
            debugInfo.append(", minOccupancy=").append(dto.minOccupancy());
        }

        if (dto.departmentId() != null) {
            existing.setDepartment(departmentService.getDepartmentById(dto.departmentId()));
            debugInfo.append(", departmentId=").append(dto.departmentId());
        }

        if (dto.buildingId() != null) {
            existing.setBuilding(buildingService.getBuildingById(dto.buildingId()));
            debugInfo.append(", buildingId=").append(dto.buildingId());
        }

        Room updatedRoom = roomRepository.save(existing);

        log.info("Updated room with id={}", id);
        log.debug(debugInfo.toString());

        return updatedRoom;
    }

    /**
     * Soft delete policy
     * @param id
     */
    @Transactional
    public void delete(Long id) {
        Room room = getById(id);

        if (room.getDepartment() != null) {
            room.getDepartment().getRooms().remove(room);
            room.setDepartment(null);
        }

        if (room.getBuilding() != null) {
            room.getBuilding().getRooms().remove(room);
            room.setBuilding(null);
        }

        for (EmployeeProfile ep : room.getEmployeeProfiles()) {
            ep.setRoom(null);
        }
        room.getEmployeeProfiles().clear();

        for (SensorStation ss : room.getSensorStations()) {
            ss.setDeviceStatus(DeviceStatus.DECOMMISSIONED);
        }

        if (room.getRaspberryPi() != null) {
            room.getRaspberryPi().setDeviceStatus(DeviceStatus.DECOMMISSIONED);
        }

        room.setActive(false);

        log.info("Soft-deleted room with id={}", id);
        log.debug("Room id={} marked inactive, associations cleared, and linked devices decommissioned", id);
    }
}