package at.qe.skeleton.mappers;

import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.dtos.ClimateHintCreateDTO;
import at.qe.skeleton.models.ClimateHint;
import org.springframework.stereotype.Service;

@Service
public class ClimateHintCreateMapper implements DTOMapper<ClimateHint, ClimateHintCreateDTO> {

    @Override
    public ClimateHint mapFrom(ClimateHintCreateDTO dto) {
        ClimateHint entity = new ClimateHint();
        entity.setMetric(dto.metric());
        entity.setHintText(dto.hintText());
        return entity;
    }

    @Override
    public ClimateHintCreateDTO mapTo(ClimateHint entity) {
        throw new UnsupportedOperationException();
    }
}
