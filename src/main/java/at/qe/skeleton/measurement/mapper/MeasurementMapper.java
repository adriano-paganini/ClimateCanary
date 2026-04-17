package at.qe.skeleton.measurement.mapper;

import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.measurement.dto.MeasurementDTO;
import at.qe.skeleton.measurement.model.Measurement;
import at.qe.skeleton.violation.model.ThresholdViolation;
import org.springframework.stereotype.Service;

@Service
public class MeasurementMapper implements DTOMapper<Measurement, MeasurementDTO> {

    @Override
    public MeasurementDTO mapTo(Measurement entity) {

        return new MeasurementDTO(
                entity.getId(),
                entity.getTimestamp(),
                entity.getMeasurement(),
                entity.getMetric(),
                entity.getRoom() == null ? null : entity.getRoom().getId(),
                entity.getSensorStation() == null ? null : entity.getSensorStation().getId(),
                entity.getThresholdViolations()
                        .stream()
                        .map(ThresholdViolation::getId)
                        .toList()
        );
    }

    @Override
    public Measurement mapFrom(MeasurementDTO dto) {
        throw new UnsupportedOperationException();
    }
}
