package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.ClimateHintUpdateDTO;
import at.qe.skeleton.models.ClimateHint;
import at.qe.skeleton.repositories.ClimateHintRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
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
                .orElseThrow(() -> new NotFoundException("Climate hint not found with id: " + id));
    }

    public ClimateHint create(ClimateHint climateHint) {
        ClimateHint savedClimateHint = climateHintRepository.save(climateHint);

        log.info("Created climate hint with id={}", savedClimateHint.getId());
        log.debug("Created climate hint details: id={}, metric={}, hintText={}",
                savedClimateHint.getId(),
                savedClimateHint.getMetric(),
                savedClimateHint.getHintText());

        return savedClimateHint;
    }

    public ClimateHint update(Long id, ClimateHintUpdateDTO dto) {
        ClimateHint climateHint = getClimateHintById(id);

        StringBuilder debugInfo = new StringBuilder("Updated climate hint details:")
                .append(" id=").append(id);

        if (dto.metric() != null) {
            climateHint.setMetric(dto.metric());
            debugInfo.append(", metric=").append(dto.metric());
        }

        if (dto.hintText() != null) {
            climateHint.setHintText(dto.hintText());
            debugInfo.append(", hintText=").append(dto.hintText());
        }

        ClimateHint updatedClimateHint = climateHintRepository.save(climateHint);

        log.info("Updated climate hint with id={}", id);
        log.debug(debugInfo.toString());

        return updatedClimateHint;
    }

    @Transactional
    public void delete(Long id) {
        ClimateHint entity = climateHintRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Climate hint not found with id: " + id));

        entity.getThresholds().clear();
        climateHintRepository.delete(entity);

        log.info("Deleted climate hint with id={}", id);
        log.debug("Cleared thresholds and deleted climate hint id={}", id);
    }
}