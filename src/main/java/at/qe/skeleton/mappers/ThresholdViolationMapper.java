package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.ThresholdViolationDTO;
import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.models.Measurement;
import at.qe.skeleton.models.ThresholdViolation;
import org.springframework.stereotype.Service;

@Service
public class ThresholdViolationMapper implements DTOMapper<ThresholdViolation, ThresholdViolationDTO> {

    @Override
    public ThresholdViolationDTO mapTo(ThresholdViolation entity) {
        if (entity == null) return null;

        return mapTo(entity, true);
    }

    public ThresholdViolationDTO mapToListItem(ThresholdViolation entity) {
        if (entity == null) return null;

        return mapTo(entity, false);
    }

    private ThresholdViolationDTO mapTo(ThresholdViolation entity, boolean includeMeasurementIds) {
        return new ThresholdViolationDTO(
                entity.getId(),
                entity.getMetric(),
                entity.getValue(),
                entity.getViolationStatus(),
                entity.getEndTime(),
                entity.getStartTime(),
                entity.getThreshold() != null ? entity.getThreshold().getId() : null,
                entity.getRoom() != null ? entity.getRoom().getId() : null,
                includeMeasurementIds
                        ? entity.getMeasurements()
                                .stream()
                                .map(Measurement::getId)
                                .toList()
                        : null
        );
    }

    @Override
    public ThresholdViolation mapFrom(ThresholdViolationDTO dto) {
        throw new UnsupportedOperationException();
    }
}
