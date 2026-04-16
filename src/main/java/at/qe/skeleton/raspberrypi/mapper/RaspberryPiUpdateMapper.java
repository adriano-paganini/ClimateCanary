package at.qe.skeleton.raspberrypi.mapper;

import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.raspberrypi.dto.RaspberryPiUpdateDTO;
import at.qe.skeleton.raspberrypi.model.RaspberryPi;
import at.qe.skeleton.room.service.RoomService;
import at.qe.skeleton.sensorstation.model.SensorStation;
import at.qe.skeleton.sensorstation.repository.SensorStationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RaspberryPiUpdateMapper implements DTOMapper<RaspberryPi, RaspberryPiUpdateDTO> {

    private final RoomService roomService;
    private final SensorStationRepository sensorStationRepository;

    public RaspberryPiUpdateMapper(RoomService roomService, SensorStationRepository sensorStationRepository) {
        this.roomService = roomService;
        this.sensorStationRepository = sensorStationRepository;
    }

    @Override
    public RaspberryPi mapFrom(RaspberryPiUpdateDTO dto) {

        RaspberryPi pi = new RaspberryPi();

        if (dto.ipAddress() != null) {
            pi.setIpAddress(dto.ipAddress());
        }

        if (dto.deviceStatus() != null) {
            pi.setDeviceStatus(dto.deviceStatus());
        }

        if (dto.roomId() != null) {
            pi.setRoom(roomService.getById(dto.roomId()));
        }

        if (dto.sensorStationIds() != null) {
            List<SensorStation> stations = sensorStationRepository.findAllById(dto.sensorStationIds());
            pi.setSensorStations(stations);
        }

        return pi;
    }

    @Override
    public RaspberryPiUpdateDTO mapTo(RaspberryPi entity) {
        throw new UnsupportedOperationException();
    }
}
