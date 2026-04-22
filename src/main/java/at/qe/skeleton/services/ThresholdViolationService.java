package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.NotFoundException;
import at.qe.skeleton.dtos.ThresholdViolationCreateDTO;
import at.qe.skeleton.dtos.ThresholdViolationUpdateDTO;
import at.qe.skeleton.mappers.ThresholdViolationCreateMapper;
import at.qe.skeleton.models.ThresholdViolation;
import at.qe.skeleton.repositories.ThresholdViolationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ThresholdViolationService {

    private final ThresholdViolationRepository thresholdViolationRepository;
    private final RoomService roomService;
    private final ThresholdService thresholdService;
    private final ThresholdViolationCreateMapper thresholdViolationCreateMapper;

    public ThresholdViolationService(ThresholdViolationRepository thresholdViolationRepository,
                                     RoomService roomService,
                                     ThresholdService thresholdService, ThresholdViolationCreateMapper thresholdViolationCreateMapper) {
        this.thresholdViolationRepository = thresholdViolationRepository;
        this.roomService = roomService;
        this.thresholdService = thresholdService;
        this.thresholdViolationCreateMapper = thresholdViolationCreateMapper;
    }

    public List<ThresholdViolation>  findAll() {
        return thresholdViolationRepository.findAll();
    }

    public ThresholdViolation findById(Long id) {
        return thresholdViolationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("There is no such threshold violation with id: " + id));
    }

    public ThresholdViolation create(ThresholdViolationCreateDTO dto) {
        ThresholdViolation entity = thresholdViolationCreateMapper.mapFrom(dto);
        return thresholdViolationRepository.save(entity);
    }

    public ThresholdViolation update(Long id, ThresholdViolationUpdateDTO dto) {
        ThresholdViolation entity = findById(id);

        if (dto.metric() != null)
            entity.setMetric(dto.metric());

        if (dto.value() != null)
            entity.setValue(dto.value());

        if (dto.violationStatus() != null)
            entity.setViolationStatus(dto.violationStatus());

        if (dto.startTime() != null)
            entity.setStartTime(dto.startTime());

        if (dto.endTime() != null)
            entity.setEndTime(dto.endTime());

        if (dto.thresholdId() != null) {
            entity.setThreshold(thresholdService.getThresholdById(dto.thresholdId()));
        }

        if (dto.roomId() != null) {
            entity.setRoom(roomService.getById(dto.roomId()));
        }

        return thresholdViolationRepository.save(entity);
    }

    public void delete(Long id) {
        thresholdViolationRepository.deleteById(id);
    }
}
