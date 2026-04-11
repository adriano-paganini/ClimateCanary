package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.DepartmentDTO;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.repositories.RoomRepository;
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

        // TODO: DepartmentLead ID, entity needs to be changed to support separate field

        return new DepartmentDTO(
                entity.getId(),
                entity.getName(),
                entity.getRooms()
                        .stream()
                        .map(Room::getId)
                        .toList(),
                null
        );
    }

    @Override
    public Department mapFrom(DepartmentDTO dto) {
        Department department = new Department();

        department.setName(dto.name());

        if (dto.roomIds() != null) {
            List<Room> rooms = roomRepository.findAllByIds(dto.roomIds());

            department.setRooms(rooms);
            rooms.forEach(room -> room.setDepartment(department));
        }

        return department;
    }
}