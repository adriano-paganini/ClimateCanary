package at.qe.skeleton.threshold.service;

import at.qe.skeleton.threshold.dto.ThresholdCreateDTO;
import at.qe.skeleton.threshold.dto.ThresholdUpdateDTO;
import at.qe.skeleton.common.exceptions.EntityInUseException;
import at.qe.skeleton.common.exceptions.ThresholdNotFoundException;
import at.qe.skeleton.threshold.mapper.ThresholdMapper;
import at.qe.skeleton.climatehint.model.ClimateHint;
import at.qe.skeleton.threshold.model.Threshold;
import at.qe.skeleton.climatehint.repository.ClimateHintRepository;
import at.qe.skeleton.threshold.repository.ThresholdRepository;
import at.qe.skeleton.room.service.RoomService;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
public class ThresholdService {

    private final ThresholdRepository thresholdRepository;
    private final RoomService roomService;
    private final ClimateHintRepository climateHintRepository;

    public ThresholdService(ThresholdRepository thresholdRepository,
                            RoomService roomService,
                            ThresholdMapper thresholdMapper,
                            ClimateHintRepository climateHintRepository) {
        this.thresholdRepository = thresholdRepository;
        this.roomService = roomService;
        this.climateHintRepository = climateHintRepository;
    }

    public List<Threshold> getAll(){
        return thresholdRepository.findAll();
    }

    public Threshold getThresholdById(Long id){
        return thresholdRepository.findById(id)
                .orElseThrow(() -> new ThresholdNotFoundException("Threshold with id " + id + " not found"));
    }

    public Threshold create(ThresholdCreateDTO dto) {
        Threshold entity = new Threshold();
        entity.setMetric(dto.metric());
        entity.setBoundValue(dto.boundValue());
        entity.setThresholdType(dto.thresholdType());
        entity.setEnabled(true);
        entity.setRoom(roomService.getById(dto.roomId()));

        if (dto.climateHintIds() != null) {
            List<ClimateHint> hints = climateHintRepository.findAllById(dto.climateHintIds());
            entity.setClimateHints(new HashSet<>(hints));
        }

        return thresholdRepository.save(entity);
    }

    public Threshold update(Long id, ThresholdUpdateDTO dto) {

        Threshold entity = thresholdRepository.findById(id)
                .orElseThrow(() -> new ThresholdNotFoundException("Threshold with id " + id + " not found"));

        if (dto.metric() != null)
            entity.setMetric(dto.metric());

        if (dto.boundValue() != null)
            entity.setBoundValue(dto.boundValue());

        if (dto.thresholdType() != null)
            entity.setThresholdType(dto.thresholdType());

        if (dto.enabled() != null)
            entity.setEnabled(dto.enabled());

        if (dto.roomId() != null) {
            entity.setRoom(roomService.getById(dto.roomId()));
        }

        if (dto.climateHintIds() != null) {
            List<ClimateHint> hints = climateHintRepository.findAllById(dto.climateHintIds());
            entity.getClimateHints().clear();
            entity.getClimateHints().addAll(hints);
        }

        return thresholdRepository.save(entity);
    }

    public void delete(Long id) {
        Threshold entity = thresholdRepository.findById(id)
                .orElseThrow(() -> new ThresholdNotFoundException("Threshold with id " + id + " not found"));

        if (!entity.getViolations().isEmpty()) {
            throw new EntityInUseException("Threshold cannot be enabled because it has violations");
        }

        entity.getClimateHints().clear();
        thresholdRepository.delete(entity);
    }

}
