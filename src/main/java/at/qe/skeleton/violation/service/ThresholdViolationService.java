package at.qe.skeleton.violation.service;

import at.qe.skeleton.violation.dto.ThresholdViolationCreateDTO;
import at.qe.skeleton.violation.dto.ThresholdViolationUpdateDTO;
import at.qe.skeleton.common.exceptions.ThresholdViolationNotFound;
import at.qe.skeleton.violation.model.ThresholdViolation;
import at.qe.skeleton.violation.model.ViolationStatus;
import at.qe.skeleton.threshold.repository.ThresholdRepository;
import at.qe.skeleton.violation.repository.ThresholdViolationRepository;
import at.qe.skeleton.room.service.RoomService;
import at.qe.skeleton.threshold.service.ThresholdService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ThresholdViolationService {

    private final ThresholdViolationRepository thresholdViolationRepository;
    private final ThresholdRepository thresholdRepository;
    private final RoomService roomService;
    private final ThresholdService thresholdService;

    public ThresholdViolationService(ThresholdViolationRepository thresholdViolationRepository,
                                     ThresholdRepository thresholdRepository,
                                     RoomService roomService, ThresholdService thresholdService) {
        this.thresholdViolationRepository = thresholdViolationRepository;
        this.thresholdRepository = thresholdRepository;
        this.roomService = roomService;
        this.thresholdService = thresholdService;
    }

    public List<ThresholdViolation>  findAll() {
        return thresholdViolationRepository.findAll();
    }

    public ThresholdViolation findById(Long id) {
        return thresholdViolationRepository.findById(id)
                .orElseThrow(() -> new ThresholdViolationNotFound("There is no such threshold violation with id: " + id));
    }

    public ThresholdViolation create(ThresholdViolationCreateDTO dto) {
        ThresholdViolation entity = new ThresholdViolation();
        entity.setMetric(dto.metric());
        entity.setValue(dto.value());
        entity.setStartTime(dto.startTime());
        entity.setThreshold(thresholdService.getThresholdById(dto.thresholdId()));
        entity.setRoom(roomService.getById(dto.roomId()));
        entity.setViolationStatus(ViolationStatus.ACTIVE);
        return thresholdViolationRepository.save(entity);
    }

    public ThresholdViolation update(Long id, ThresholdViolationUpdateDTO dto) {

        ThresholdViolation entity = thresholdViolationRepository.findById(id)
                .orElseThrow(() -> new ThresholdViolationNotFound("There is no such threshold violation with id: " + id));

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

        return entity;
    }

    public void delete(Long id) {
        thresholdViolationRepository.deleteById(id);
    }
}
