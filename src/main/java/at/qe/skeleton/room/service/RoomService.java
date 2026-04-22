package at.qe.skeleton.room.service;

import at.qe.skeleton.building.service.BuildingService;
import at.qe.skeleton.common.exceptions.RoomNotFoundException;
import at.qe.skeleton.department.service.DepartmentService;
import at.qe.skeleton.employeeprofile.model.EmployeeProfile;
import at.qe.skeleton.employeeprofile.repository.EmployeeProfileRepository;
import at.qe.skeleton.raspberrypi.model.DeviceStatus;
import at.qe.skeleton.room.dto.RoomUpdateDTO;
import at.qe.skeleton.room.model.Room;
import at.qe.skeleton.room.repository.RoomRepository;
import at.qe.skeleton.sensorstation.model.SensorStation;
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
    }
}
