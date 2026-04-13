package at.qe.skeleton.department.mapper;

import at.qe.skeleton.department.dto.DepartmentDTO;
import at.qe.skeleton.department.model.Department;
import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.room.model.Room;
import at.qe.skeleton.room.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentMapper implements DTOMapper<Department, DepartmentDTO> {

    private final RoomRepository roomRepository;

    public DepartmentMapper(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public DepartmentDTO mapTo(Department entity) {

        return new DepartmentDTO(
                entity.getId(),
                entity.getName(),
                entity.getRooms()
                        .stream()
                        .map(Room::getId)
                        .toList(),
                entity.getDepartmentLeader() != null ? entity.getDepartmentLeader().getId() : null
        );
    }

    @Override
    public Department mapFrom(DepartmentDTO dto) {
        Department department = new Department();

        department.setName(dto.name());

        if (dto.roomIds() != null) {
            List<Room> rooms = roomRepository.findAllByIdsAndActiveTrue(dto.roomIds());

            department.setRooms(rooms);
            rooms.forEach(room -> room.setDepartment(department));
        }

        return department;
    }
}