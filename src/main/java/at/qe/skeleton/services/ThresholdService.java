package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.ConflictException;
import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.ThresholdCreateDTO;
import at.qe.skeleton.dtos.ThresholdUpdateDTO;
import at.qe.skeleton.models.ClimateHint;
import at.qe.skeleton.models.Threshold;
import at.qe.skeleton.repositories.ClimateHintRepository;
import at.qe.skeleton.repositories.ThresholdRepository;
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
                .orElseThrow(() -> new NotFoundException("Threshold with id " + id + " not found"));
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
        Threshold entity = getThresholdById(id);

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
        Threshold entity = getThresholdById(id);

        if (!entity.getViolations().isEmpty()) {
            throw new ConflictException("Threshold cannot be enabled because it has violations");
        }

        entity.getClimateHints().clear();
        thresholdRepository.delete(entity);
    }

}
