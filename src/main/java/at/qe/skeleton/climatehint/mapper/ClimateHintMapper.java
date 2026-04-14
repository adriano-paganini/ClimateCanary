package at.qe.skeleton.climatehint.mapper;

import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.climatehint.dto.ClimateHintDTO;
import at.qe.skeleton.climatehint.model.ClimateHint;
import org.springframework.stereotype.Service;

@Service
public class ClimateHintMapper implements DTOMapper<ClimateHint, ClimateHintDTO> {

    @Override
    public ClimateHintDTO mapTo(ClimateHint entity) {
        return new ClimateHintDTO(
                entity.getId(),
                entity.getMetric(),
                entity.getHintText()
        );
    }

    @Override
    public ClimateHint mapFrom(ClimateHintDTO dto) {
        ClimateHint entity = new ClimateHint();
        entity.setMetric(dto.metric());
        entity.setHintText(dto.hintText());
        return entity;
    }
}
