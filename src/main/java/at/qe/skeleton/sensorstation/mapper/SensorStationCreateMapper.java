package at.qe.skeleton.sensorstation.mapper;

import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.raspberrypi.service.RaspberryPiService;
import at.qe.skeleton.room.service.RoomService;
import at.qe.skeleton.sensorstation.dto.SensorStationCreateDTO;
import at.qe.skeleton.sensorstation.model.SensorStation;
import org.springframework.stereotype.Service;

@Service
public class SensorStationCreateMapper implements DTOMapper<SensorStation, SensorStationCreateDTO> {

    private final RaspberryPiService raspberryPiService;
    private final RoomService roomService;

    public SensorStationCreateMapper(RaspberryPiService raspberryPiService, RoomService roomService) {
        this.raspberryPiService = raspberryPiService;
        this.roomService = roomService;
    }

    @Override
    public SensorStation mapFrom(SensorStationCreateDTO dto) {
        SensorStation station = new SensorStation();
        station.setName(dto.name());
        station.setDeviceStatus(dto.deviceStatus());
        station.setMeasurementsPerSec(dto.measurementsPerSec());
        station.setRaspberryPi(raspberryPiService.getById(dto.raspberryPiId()));
        station.setRoom(roomService.getById(dto.roomId()));
        return station;
    }

    @Override
    public SensorStationCreateDTO mapTo(SensorStation entity) {
        throw new UnsupportedOperationException();
    }
}
