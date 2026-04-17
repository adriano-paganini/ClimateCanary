package at.qe.skeleton.raspberrypi.mapper;

import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.raspberrypi.dto.RaspberryPiCreateDTO;
import at.qe.skeleton.raspberrypi.model.RaspberryPi;
import at.qe.skeleton.room.service.RoomService;
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
        return pi;
    }

    @Override
    public RaspberryPiCreateDTO mapTo(RaspberryPi entity) {
        throw new UnsupportedOperationException();
    }
}
