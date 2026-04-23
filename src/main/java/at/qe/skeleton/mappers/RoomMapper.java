package at.qe.skeleton.mappers;

import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.dtos.RoomDTO;
import at.qe.skeleton.models.Room;
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
                entity.getDepartment() == null ? null : entity.getDepartment().getId(),
                entity.getBuilding() == null ? null : entity.getBuilding().getId(),
                entity.isActive()
        );
    }

    @Override
    public Room mapFrom(RoomDTO dto) {
        throw new UnsupportedOperationException();
    }
}
