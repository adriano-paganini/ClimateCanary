package at.qe.skeleton.sensorstation.mapper;

import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.sensorstation.dto.SensorStationDTO;
import at.qe.skeleton.sensorstation.model.SensorStation;
import org.springframework.stereotype.Service;

@Service
public class SensorStationMapper implements DTOMapper<SensorStation, SensorStationDTO> {

    @Override
    public SensorStationDTO mapTo(SensorStation entity) {

        return new SensorStationDTO(
                entity.getId(),
                entity.getName(),
                entity.getDeviceStatus(),
                entity.getMeasurementsPerSec(),
                entity.getRaspberryPi() == null ? null : entity.getRaspberryPi().getId()
        );
    }

    @Override
    public SensorStation mapFrom(SensorStationDTO dto) {
        throw new UnsupportedOperationException();
    }
}