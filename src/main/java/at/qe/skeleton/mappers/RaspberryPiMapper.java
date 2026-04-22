package at.qe.skeleton.mappers;

import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.dtos.RaspberryPiDTO;
import at.qe.skeleton.models.RaspberryPi;
import at.qe.skeleton.models.SensorStation;
import org.springframework.stereotype.Service;

@Service
public class RaspberryPiMapper implements DTOMapper<RaspberryPi, RaspberryPiDTO> {

    @Override
    public RaspberryPiDTO mapTo(RaspberryPi entity) {
        return new RaspberryPiDTO(
                entity.getId(),
                entity.getHostName(),
                entity.getIpAddress(),
                entity.getDeviceStatus(),
                entity.getSensorStations()
                        .stream()
                        .map(SensorStation::getId)
                        .toList()
        );
    }

    @Override
    public RaspberryPi mapFrom(RaspberryPiDTO dto) {
        throw new UnsupportedOperationException();
    }
}
