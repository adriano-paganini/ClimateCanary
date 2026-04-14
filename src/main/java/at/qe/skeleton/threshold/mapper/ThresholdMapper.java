package at.qe.skeleton.threshold.mapper;

import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.threshold.dto.ThresholdDTO;
import at.qe.skeleton.climatehint.model.ClimateHint;
import at.qe.skeleton.threshold.model.Threshold;
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
