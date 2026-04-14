package at.qe.skeleton.climatehint.mapper;

import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.climatehint.dto.ClimateHintCreateDTO;
import at.qe.skeleton.climatehint.model.ClimateHint;
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
