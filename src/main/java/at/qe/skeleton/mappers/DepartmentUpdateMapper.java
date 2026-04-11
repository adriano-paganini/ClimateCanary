package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.DepartmentCreateDTO;
import at.qe.skeleton.dtos.DepartmentUpdateDTO;
import at.qe.skeleton.model.Department;
import at.qe.skeleton.model.Room;
import at.qe.skeleton.repositories.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentUpdateMapper implements DTOMapper<Department, DepartmentUpdateDTO> {

    private final RoomRepository roomRepository;

    public  DepartmentUpdateMapper(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public DepartmentUpdateDTO mapTo(Department entity) {
        throw new  UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Department mapFrom(DepartmentUpdateDTO dto) {
        Department department = new Department();
        department.setName(dto.name());

        if (dto.roomIds() != null) {
            List<Room> rooms = roomRepository.findAllById(dto.roomIds());

            department.setRooms(rooms);
            rooms.forEach(room -> room.setDepartment(department));
        }

        return department;
    }
}
