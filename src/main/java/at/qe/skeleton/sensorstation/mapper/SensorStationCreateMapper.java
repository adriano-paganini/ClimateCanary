package at.qe.skeleton.sensorstation.mapper;

import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.raspberrypi.model.RaspberryPi;
import at.qe.skeleton.raspberrypi.repository.RaspberryPiRepository;
import at.qe.skeleton.sensorstation.dto.SensorStationCreateDTO;
import at.qe.skeleton.sensorstation.model.SensorStation;
import org.springframework.stereotype.Service;

@Service
public class SensorStationCreateMapper implements DTOMapper<SensorStation, SensorStationCreateDTO> {

    private final RaspberryPiRepository raspberryPiRepository;

    public SensorStationCreateMapper(RaspberryPiRepository raspberryPiRepository) {
        this.raspberryPiRepository = raspberryPiRepository;
    }

    @Override
    public SensorStation mapFrom(SensorStationCreateDTO dto) {
        SensorStation station = new SensorStation();
        station.setName(dto.name());
        station.setDeviceStatus(dto.deviceStatus());
        station.setMeasurementsPerSec(dto.measurementsPerSec());

        RaspberryPi pi = raspberryPiRepository.findById(dto.raspberryPiId())
                .orElseThrow(() -> new RuntimeException("RaspberryPi not found"));

        station.setRaspberryPi(pi);

        return station;
    }

    @Override
    public SensorStationCreateDTO mapTo(SensorStation entity) {
        throw new UnsupportedOperationException();
    }
}
