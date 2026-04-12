package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.RoomDTO;
import at.qe.skeleton.model.Room;
import org.springframework.stereotype.Service;

@Service
public class RoomMapper implements DTOMapper<Room, RoomDTO> {

    @Override
    public RoomDTO mapTo(Room entity) {

        return new RoomDTO(
                entity.getId(),
                entity.getName(),
                entity.getRoomType(),
                entity.getMinOccupancy(),
                entity.getDepartment().getId(),
                entity.getBuilding().getId()
        );
    }

    @Override
    public Room mapFrom(RoomDTO dto) {
        throw new UnsupportedOperationException();
    }
}
