package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.ThresholdViolationDTO;
import at.qe.skeleton.model.ThresholdViolation;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ThresholdViolationMapper implements DTOMapper<ThresholdViolation, ThresholdViolationDTO> {

    @Override
    public ThresholdViolationDTO mapTo(ThresholdViolation entity) {
        if (entity == null) return null;

        return new ThresholdViolationDTO(
                entity.getId(),
                entity.getMetric(),
                entity.getValue(),
                entity.getViolationStatus(),
                entity.getEndTime(),
                entity.getStartTime(),
                entity.getThreshold() != null ? entity.getThreshold().getId() : null,
                entity.getRoom() != null ? entity.getRoom().getId() : null,
                List.of()
        );
    }

    @Override
    public ThresholdViolation mapFrom(ThresholdViolationDTO dto) {
        throw new UnsupportedOperationException();
    }
}
