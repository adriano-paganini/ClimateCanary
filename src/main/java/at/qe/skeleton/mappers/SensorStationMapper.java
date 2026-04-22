package at.qe.skeleton.mappers;

import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.dtos.SensorStationDTO;
import at.qe.skeleton.models.SensorStation;
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
                entity.getRaspberryPi() == null ? null : entity.getRaspberryPi().getId(),
                entity.getRoom() == null ? null : entity.getRoom().getId()
        );
    }

    @Override
    public SensorStation mapFrom(SensorStationDTO dto) {
        throw new UnsupportedOperationException();
    }
}