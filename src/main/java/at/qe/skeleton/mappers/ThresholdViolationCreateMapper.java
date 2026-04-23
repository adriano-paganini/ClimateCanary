package at.qe.skeleton.mappers;

import at.qe.skeleton.common.DTOMapper;
import at.qe.skeleton.dtos.ThresholdViolationCreateDTO;
import at.qe.skeleton.models.ThresholdViolation;
import at.qe.skeleton.models.ViolationStatus;
import at.qe.skeleton.services.RoomService;
import at.qe.skeleton.services.ThresholdService;
import org.springframework.stereotype.Service;

@Service
public class ThresholdViolationCreateMapper implements DTOMapper<ThresholdViolation, ThresholdViolationCreateDTO> {

    private final ThresholdService thresholdService;
    private final RoomService roomService;

    public ThresholdViolationCreateMapper(
            ThresholdService thresholdService,
            RoomService roomService) {
        this.thresholdService = thresholdService;
        this.roomService = roomService;
    }

    @Override
    public ThresholdViolation mapFrom(ThresholdViolationCreateDTO dto) {
        ThresholdViolation entity = new ThresholdViolation();
        entity.setMetric(dto.metric());
        entity.setValue(dto.value());
        entity.setStartTime(dto.startTime());
        entity.setThreshold(thresholdService.getThresholdById(dto.thresholdId()));
        entity.setRoom(roomService.getById(dto.roomId()));
        entity.setViolationStatus(ViolationStatus.ACTIVE);
        return entity;
    }

    @Override
    public ThresholdViolationCreateDTO mapTo(ThresholdViolation entity) {
        throw new UnsupportedOperationException();
    }
}
