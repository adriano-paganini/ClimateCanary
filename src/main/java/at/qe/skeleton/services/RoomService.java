package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.models.EmployeeProfile;
import at.qe.skeleton.dtos.RoomUpdateDTO;
import at.qe.skeleton.models.DeviceStatus;
import at.qe.skeleton.models.Room;
import at.qe.skeleton.models.SensorStation;
import at.qe.skeleton.repositories.RoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final DepartmentService departmentService;
    private final BuildingService buildingService;
    private final EmployeeProfileService employeeProfileService;

    public RoomService(RoomRepository repo,
                       DepartmentService departmentService,
                       BuildingService buildingService, EmployeeProfileService employeeProfileService) {
        this.roomRepository = repo;
        this.departmentService = departmentService;
        this.buildingService = buildingService;
        this.employeeProfileService = employeeProfileService;
    }

    public List<Room> getAll() {
        return roomRepository.findAllByActiveTrue();
    }

    public Room getById(Long id) {
        return roomRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Room with id " + id + " not found"));
    }

    public Set<Room> getRoomsByUserIds(List<Long> userIds){
        Set<Room> rooms = new HashSet<>();
        List<EmployeeProfile> profiles= employeeProfileService.getAll(null,null);
        for(Long userId : userIds){
            if (profiles.stream().anyMatch(ep -> ep.getUser().getId().equals(userId))) {
                rooms.add(profiles.stream().filter(ep -> ep.getUser().getId().equals(userId)).findFirst().get().getRoom());
            }
        }
        return rooms;
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

    /**
     * Soft delete policy
     * @param id
     */
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
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