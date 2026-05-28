package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.ConflictException;
import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.RoomUpdateDTO;
import at.qe.skeleton.models.DeviceStatus;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.models.SensorStation;
import at.qe.skeleton.repositories.EmployeeProfileRepository;
import at.qe.skeleton.repositories.RoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final DepartmentService departmentService;
    private final BuildingService buildingService;
    private final EmployeeProfileRepository employeeProfileRepository;

    public RoomService(RoomRepository repo,
                       DepartmentService departmentService,
                       BuildingService buildingService,
                       EmployeeProfileRepository employeeProfileRepository) {
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
                .orElseThrow(() -> new NotFoundException("Room with id " + id + " not found"));
    }

    public Room getByIdInternal(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Room with id " + id + " not found"));
    }

    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public Room create(Room room) {
        Room savedRoom = roomRepository.save(room);

        log.info("Created room with id={}", savedRoom.getId());
        log.debug("Created room details: id={}, name={}, roomType={}, privacyMode={}, departmentId={}, buildingId={}",
                savedRoom.getId(),
                savedRoom.getName(),
                savedRoom.getRoomType(),
                savedRoom.getPrivacyMode(),
                savedRoom.getDepartment() != null ? savedRoom.getDepartment().getId() : null,
                savedRoom.getBuilding() != null ? savedRoom.getBuilding().getId() : null);

        return savedRoom;
    }

    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
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

        if (dto.privacyMode() != null) {
            existing.setPrivacyMode(dto.privacyMode());
            debugInfo.append(", privacyMode=").append(dto.privacyMode());
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

    public Room updatePrivacyModeInternal(Long id, boolean privacyMode) {
        Room room = getById(id);
        room.setPrivacyMode(privacyMode);
        Room updatedRoom = roomRepository.save(room);
        log.info("Updated privacy mode for room id={} to {}", id, privacyMode);
        return updatedRoom;
    }

    /**
     * Soft delete policy
     * @param id the id of the room to delete
     */
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Transactional
    public void delete(Long id) {
        Room room = getById(id);
        long assignedEmployeeCount = employeeProfileRepository.countByRoom_Id(id);
        if (assignedEmployeeCount > 0) {
            throw new ConflictException("Room with id " + id + " has " + assignedEmployeeCount + " assigned employees");
        }

        for (SensorStation ss : room.getSensorStations()) {
            ss.setDeviceStatus(DeviceStatus.DECOMMISSIONED);
        }

        if (room.getRaspberryPi() != null) {
            room.getRaspberryPi().setDeviceStatus(DeviceStatus.DECOMMISSIONED);
        }

        if (room.getDepartment() != null) {
            room.getDepartment().getRooms().remove(room);
            room.setDepartment(null);
        }

        if (room.getBuilding() != null) {
            room.getBuilding().getRooms().remove(room);
            room.setBuilding(null);
        }

        room.getEmployeeProfiles().clear();

        room.setActive(false);
        roomRepository.save(room);

        log.info("Soft-deleted room with id={}", id);
        log.debug("Room id={} marked inactive and linked devices decommissioned", id);
    }
}
