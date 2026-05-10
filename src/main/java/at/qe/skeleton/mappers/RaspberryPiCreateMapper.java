package at.qe.skeleton.mappers;

import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.dtos.RaspberryPiCreateDTO;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.models.Room;
import org.springframework.stereotype.Service;

@Service
public class RaspberryPiCreateMapper implements DTOMapper<RaspberryPi, RaspberryPiCreateDTO> {



    @Override
    public RaspberryPi mapFrom(RaspberryPiCreateDTO dto) {
        RaspberryPi pi = new RaspberryPi();
        Room room = new Room();
        room.setId(dto.roomId());
        pi.setRoom(room);
        pi.setHostName(dto.hostName());
        return pi;
    }

    @Override
    public RaspberryPiCreateDTO mapTo(RaspberryPi entity) {
        throw new UnsupportedOperationException();
    }
}
