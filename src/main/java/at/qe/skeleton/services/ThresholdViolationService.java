package at.qe.skeleton.services;

import at.qe.skeleton.common.exceptions.ThresholdViolationNotFound;
import at.qe.skeleton.dtos.ThresholdViolationCreateDTO;
import at.qe.skeleton.dtos.ThresholdViolationUpdateDTO;
import at.qe.skeleton.models.ThresholdViolation;
import at.qe.skeleton.models.ViolationStatus;
import at.qe.skeleton.repositories.ThresholdViolationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ThresholdViolationService {

    private final ThresholdViolationRepository thresholdViolationRepository;
    private final RoomService roomService;
    private final ThresholdService thresholdService;

    public ThresholdViolationService(ThresholdViolationRepository thresholdViolationRepository,
                                     RoomService roomService,
                                     ThresholdService thresholdService) {
        this.thresholdViolationRepository = thresholdViolationRepository;
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

        ThresholdViolation savedViolation = thresholdViolationRepository.save(entity);

        log.info("Created threshold violation with id={}", savedViolation.getId());
        log.debug("Created threshold violation details: id={}, metric={}, value={}, startTime={}, thresholdId={}, roomId={}, violationStatus={}",
                savedViolation.getId(),
                savedViolation.getMetric(),
                savedViolation.getValue(),
                savedViolation.getStartTime(),
                savedViolation.getThreshold() != null ? savedViolation.getThreshold().getId() : null,
                savedViolation.getRoom() != null ? savedViolation.getRoom().getId() : null,
                savedViolation.getViolationStatus());

        return savedViolation;
    }

    public ThresholdViolation update(Long id, ThresholdViolationUpdateDTO dto) {

        ThresholdViolation entity = thresholdViolationRepository.findById(id)
                .orElseThrow(() -> new ThresholdViolationNotFound("There is no such threshold violation with id: " + id));

        StringBuilder debugInfo = new StringBuilder("Updated threshold violation details:")
                .append(" id=").append(id);

        if (dto.metric() != null) {
            entity.setMetric(dto.metric());
            debugInfo.append(", metric=").append(dto.metric());
        }

        if (dto.value() != null) {
            entity.setValue(dto.value());
            debugInfo.append(", value=").append(dto.value());
        }

        if (dto.violationStatus() != null) {
            entity.setViolationStatus(dto.violationStatus());
            debugInfo.append(", violationStatus=").append(dto.violationStatus());
        }

        if (dto.startTime() != null) {
            entity.setStartTime(dto.startTime());
            debugInfo.append(", startTime=").append(dto.startTime());
        }

        if (dto.endTime() != null) {
            entity.setEndTime(dto.endTime());
            debugInfo.append(", endTime=").append(dto.endTime());
        }

        if (dto.thresholdId() != null) {
            entity.setThreshold(thresholdService.getThresholdById(dto.thresholdId()));
            debugInfo.append(", thresholdId=").append(dto.thresholdId());
        }

        if (dto.roomId() != null) {
            entity.setRoom(roomService.getById(dto.roomId()));
            debugInfo.append(", roomId=").append(dto.roomId());
        }

        ThresholdViolation updatedViolation = thresholdViolationRepository.save(entity);

        log.info("Updated threshold violation with id={}", id);
        log.debug(debugInfo.toString());

        return updatedViolation;
    }

    public void delete(Long id) {
        thresholdViolationRepository.deleteById(id);
        log.info("Deleted threshold violation with id={}", id);
    }
}