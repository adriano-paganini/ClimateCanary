package at.qe.skeleton.services;

import at.qe.skeleton.dtos.ClimateHintUpdateDTO;
import at.qe.skeleton.common.exceptions.ClimateHintNotFound;
import at.qe.skeleton.models.ClimateHint;
import at.qe.skeleton.repositories.ClimateHintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClimateHintService {

    private final ClimateHintRepository climateHintRepository;

    public ClimateHintService(ClimateHintRepository climateHintRepository) {
        this.climateHintRepository = climateHintRepository;
    }

    public List<ClimateHint> findAll() {
        return climateHintRepository.findAll();
    }

    public ClimateHint getClimateHintById(Long id) {
        return climateHintRepository.findById(id)
                .orElseThrow(() -> new ClimateHintNotFound("Climate hint not found with id: " + id));
    }

    public ClimateHint create(ClimateHint climateHint) {
        return climateHintRepository.save(climateHint);
    }

    public ClimateHint update(Long id, ClimateHintUpdateDTO dto) {
        ClimateHint climateHint = getClimateHintById(id);

        if (dto.metric() != null)
            climateHint.setMetric(dto.metric());

        if (dto.hintText() != null)
            climateHint.setHintText(dto.hintText());

        return climateHintRepository.save(climateHint);
    }

    @Transactional
    public void delete(Long id) {
        ClimateHint entity = climateHintRepository.findById(id)
                .orElseThrow(() -> new ClimateHintNotFound("Climate hint not found with id: " + id));

        entity.getThresholds().clear();
        climateHintRepository.delete(entity);
    }
}
