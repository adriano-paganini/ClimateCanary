package at.qe.skeleton.mappers;

import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.dtos.RaspberryPiCreateDTO;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.services.RoomService;
import org.springframework.stereotype.Service;

@Service
public class RaspberryPiCreateMapper implements DTOMapper<RaspberryPi, RaspberryPiCreateDTO> {

    private final RoomService roomService;

    public RaspberryPiCreateMapper(RoomService roomService) {
        this.roomService = roomService;
    }

    @Override
    public RaspberryPi mapFrom(RaspberryPiCreateDTO dto) {
        RaspberryPi pi = new RaspberryPi();
        pi.setRoom(roomService.getById(dto.roomId()));
        pi.setHostName(dto.hostName());
        return pi;
    }


    @Override
    public RaspberryPiCreateDTO mapTo(RaspberryPi entity) {
        throw new UnsupportedOperationException();
    }
}
