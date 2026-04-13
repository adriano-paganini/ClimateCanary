package at.qe.skeleton.mappers;

import at.qe.skeleton.dtos.ThresholdDTO;
import at.qe.skeleton.model.ClimateHint;
import at.qe.skeleton.model.Threshold;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ThresholdMapper implements DTOMapper<Threshold, ThresholdDTO> {

    @Override
    public ThresholdDTO mapTo(Threshold entity) {
        List<Long> hintIds = entity.getClimateHints()
                .stream()
                .map(ClimateHint::getId)
                .toList();

        return new ThresholdDTO(
                entity.getId(),
                entity.getRoom() != null ? entity.getRoom().getId() : null,
                entity.getMetric(),
                entity.getBoundValue(),
                entity.getThresholdType(),
                hintIds,
                entity.isEnabled()
        );
    }

    @Override
    public Threshold mapFrom(ThresholdDTO dto) {
        throw new UnsupportedOperationException();
    }
}
